package iuh.igc.service.template;

import iuh.igc.dto.request.template.TemplateFieldRequest;
import iuh.igc.entity.template.TemplateDocument;
import iuh.igc.entity.template.TemplateField;
import iuh.igc.repository.TemplateRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import iuh.igc.config.s3.S3Service;
import iuh.igc.dto.response.template.TemplateResponse;
import iuh.igc.entity.User;
import iuh.igc.entity.constant.OrganizationRole;
import iuh.igc.repository.OrganizationMemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import iuh.igc.service.user.CurrentUserProvider;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateService {
    private static final long MAX_TEMPLATE_FILE_SIZE = 20 * 1024 * 1024L;
    private static final String SIGNATURE_FIELD_NAME = "signature";

    final TemplateRepository templateRepository;
    final OrganizationMemberRepository organizationMemberRepository;
    final CurrentUserProvider currentUserProvider;
    final S3Service s3Service;

    public TemplateDocument createTemplateWithPdf(Long orgId,
                                                  String name,
                                                  MultipartFile pdfFile,
                                                  List<TemplateFieldRequest> fields) {
        validateOrganizationPermission(orgId);
        validateTemplatePayload(name, fields);
        validatePdfFile(pdfFile);

        String storageKey = s3Service.uploadFile(
                pdfFile,
                "templates/org-" + orgId,
                false,
                MAX_TEMPLATE_FILE_SIZE
        );

        TemplateDocument doc = TemplateDocument.builder()
                .orgId(orgId)
                .name(name.trim())
                .pdfStorageKey(storageKey)
                .fields(toTemplateFields(fields))
                .build();

        return templateRepository.save(doc);
    }

    public TemplateDocument createTemplate(Long orgId, String name, String pdfStorageKey){
        validateOrganizationPermission(orgId);
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Template name is required");
        }
        if (!StringUtils.hasText(pdfStorageKey)) {
            throw new IllegalArgumentException("pdfStorageKey is required");
        }

        TemplateDocument doc = TemplateDocument.builder()
                .orgId(orgId)
                .name(name.trim())
                .pdfStorageKey(pdfStorageKey.trim())
                .build();

        return templateRepository.save(doc);
    }

    public TemplateDocument saveSchema(String templateId, Long orgId, List<TemplateFieldRequest> fields){
        validateOrganizationPermission(orgId);
        validateSchema(fields);

        TemplateDocument doc = templateRepository.findByIdAndOrgId(templateId, orgId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        doc.setFields(toTemplateFields(fields));
        return templateRepository.save(doc);
    }

    public TemplateDocument updateTemplateName(String templateId, Long orgId, String name) {
        validateOrganizationPermission(orgId);
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Template name is required");
        }

        TemplateDocument doc = getById(templateId, orgId);
        doc.setName(name.trim());
        return templateRepository.save(doc);
    }

    public TemplateDocument getById(String templateId, Long orgId) {
        validateOrganizationPermission(orgId);
        return getByIdForOrganization(templateId, orgId);
    }

    public TemplateDocument getByIdForOrganization(String templateId, Long orgId) {
        return templateRepository.findByIdAndOrgId(templateId, orgId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    public List<TemplateDocument> getAll(Long orgId, String keyword) {
        validateOrganizationPermission(orgId);

        if (!StringUtils.hasText(keyword)) {
            return templateRepository.findByOrgId(orgId);
        }
        return templateRepository.findByOrgIdAndNameContainingIgnoreCase(orgId, keyword.trim());
    }

    public byte[] downloadTemplatePdf(String templateId, Long orgId) {
        TemplateDocument doc = getById(templateId, orgId);
        if (!StringUtils.hasText(doc.getPdfStorageKey())) {
            throw new IllegalArgumentException("Template does not have PDF storage key");
        }
        return s3Service.downloadFileAsBytes(doc.getPdfStorageKey());
    }

    public byte[] generateExcelTemplate(String templateId, Long orgId) {
        TemplateDocument doc = getById(templateId, orgId);
        List<TemplateField> fields = Optional.ofNullable(doc.getFields()).orElseGet(ArrayList::new);

        LinkedHashSet<String> headers = new LinkedHashSet<>();
        headers.add("certificateId");

        for (TemplateField field : fields) {
            if (field == null || !StringUtils.hasText(field.getName())) {
                continue;
            }
            if ("image".equalsIgnoreCase(field.getType())) {
                continue;
            }
            headers.add(field.getName().trim());
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            var sheet = workbook.createSheet("template-data");

            Row headerRow = sheet.createRow(0);
            int colIndex = 0;
            for (String header : headers) {
                headerRow.createCell(colIndex++).setCellValue(header);
            }

            Row sampleRow = sheet.createRow(1);
            colIndex = 0;
            for (String header : headers) {
                sampleRow.createCell(colIndex++).setCellValue(sampleValueForHeader(header));
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate excel template", e);
        }
    }

    public void deleteTemplate(String templateId, Long orgId) {
        validateOrganizationPermission(orgId);
        TemplateDocument doc = getById(templateId, orgId);

        if (StringUtils.hasText(doc.getPdfStorageKey())) {
            s3Service.deleteFileByKey(doc.getPdfStorageKey());
        }
        templateRepository.delete(doc);
    }

    public TemplateResponse toResponse(TemplateDocument doc) {
        String pdfUrl = null;
        if (doc.getId() != null && doc.getOrgId() != null) {
            pdfUrl = "/api/templates/" + doc.getId() + "/pdf?orgId=" + doc.getOrgId();
        }

        return new TemplateResponse(
                doc.getId(),
                doc.getOrgId(),
                doc.getName(),
                doc.getPdfStorageKey(),
                pdfUrl,
                Optional.ofNullable(doc.getFields()).orElseGet(ArrayList::new),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }

    private void validateOrganizationPermission(Long orgId) {
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }

        User user = currentUserProvider.get();
        Collection<OrganizationRole> allowedRoles = List.of(OrganizationRole.OWNER, OrganizationRole.MODERATOR);

        boolean hasPermission = organizationMemberRepository.existsByOrganization_IdAndUser_IdAndOrganizationRoleIn(
                orgId,
                user.getId(),
                allowedRoles
        );

        if (!hasPermission) {
            throw new AccessDeniedException("You are not allowed to manage templates of this organization");
        }
    }

    private void validateTemplatePayload(String name, List<TemplateFieldRequest> fields) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Template name is required");
        }
        validateSchema(fields);
    }

    private void validatePdfFile(MultipartFile pdfFile) {
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new IllegalArgumentException("Template PDF is required");
        }

        String contentType = pdfFile.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            throw new IllegalArgumentException("Template file must be PDF");
        }
    }

    private void validateSchema(List<TemplateFieldRequest> fields) {
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("Template schema must have at least one field");
        }

        Set<String> uniqueNames = new HashSet<>();
        int imageFieldCount = 0;

        for (TemplateFieldRequest field : fields) {
            if (field == null || !StringUtils.hasText(field.name())) {
                throw new IllegalArgumentException("Schema contains invalid field name");
            }

            String normalizedName = field.name().trim().toLowerCase(Locale.ROOT);
            if (!uniqueNames.add(normalizedName)) {
                throw new IllegalArgumentException("Duplicated field name: " + field.name());
            }

            if ("image".equalsIgnoreCase(field.type())) {
                imageFieldCount++;
                if (!SIGNATURE_FIELD_NAME.equalsIgnoreCase(field.name().trim())) {
                    throw new IllegalArgumentException("Image field name must be 'signature'");
                }
            }
        }

        if (imageFieldCount > 1) {
            throw new IllegalArgumentException("Schema only supports one image field");
        }
    }

    private List<TemplateField> toTemplateFields(List<TemplateFieldRequest> fields) {
        return fields.stream()
                .filter(Objects::nonNull)
                .map(f -> TemplateField.builder()
                        .id(StringUtils.hasText(f.id()) ? f.id().trim() : UUID.randomUUID().toString())
                        .name(f.name().trim())
                        .type(f.type().trim().toLowerCase(Locale.ROOT))
                        .x(f.x())
                        .y(f.y())
                        .w(f.w())
                        .h(f.h())
                        .fontSize(f.fontSize())
                        .fontFamily(f.fontFamily())
                        .align(f.align())
                        .color(f.color())
                        .build())
                .toList();
    }

    private String sampleValueForHeader(String header) {
        String normalized = header == null ? "" : header.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "certificateid" -> "CERT-2026-0001";
            case "dateofbirth", "issuedate" -> "2000-01-15";
            case "graduationyear" -> "2026";
            case "gpa" -> "3.50";
            case "certificatetype" -> "Chung chi";
            default -> "";
        };
    }


}
