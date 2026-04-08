package iuh.igc.service.template;

import iuh.igc.config.s3.S3Service;
import iuh.igc.dto.response.template.TemplateBatchProgressResponse;
import iuh.igc.dto.response.template.TemplateBatchRowErrorResponse;
import iuh.igc.dto.response.template.TemplateBatchStartResponse;
import iuh.igc.entity.Certificate;
import iuh.igc.entity.User;
import iuh.igc.entity.constant.CertificateStatus;
import iuh.igc.entity.constant.OrganizationRole;
import iuh.igc.entity.organization.Organization;
import iuh.igc.entity.template.TemplateDocument;
import iuh.igc.entity.template.TemplateField;
import iuh.igc.repository.CertificateRepository;
import iuh.igc.repository.OrganizationMemberRepository;
import iuh.igc.repository.OrganizationRepository;
import iuh.igc.service.core.BlockchainService;
import iuh.igc.service.pdf.DigitalSignatureService;
import iuh.igc.service.pdf.HashService;
import iuh.igc.service.pdf.PdfService;
import iuh.igc.service.user.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateBatchCertificateService {

    private static final long MAX_EXCEL_SIZE = 10 * 1024 * 1024L;
    private static final long MAX_SIGNATURE_IMAGE_SIZE = 3 * 1024 * 1024L;
    private static final long MAX_USER_CERT_SIZE = 5 * 1024 * 1024L;
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    final TemplateService templateService;
    final CertificateRepository certificateRepository;
    final OrganizationRepository organizationRepository;
    final OrganizationMemberRepository organizationMemberRepository;
    final CurrentUserProvider currentUserProvider;
    final PdfService pdfService;
    final DigitalSignatureService digitalSignatureService;
    final HashService hashService;
    final S3Service s3Service;
    final BlockchainService blockchainService;

    final Map<String, BatchProgress> progressStore = new ConcurrentHashMap<>();

    @Value("${aws.s3.domain}")
    String s3Domain;

    public TemplateBatchStartResponse startBatchGeneration(String templateId,
                                                           Long orgId,
                                                           MultipartFile excelFile,
                                                           MultipartFile signatureImage,
                                                           MultipartFile userCertificate,
                                                           String certificatePassword) {
        validateInput(orgId, excelFile, signatureImage, userCertificate, certificatePassword);
        validateOrganizationPermission(orgId);
        String operatorName = currentUserProvider.get().getName();

        String batchId = UUID.randomUUID().toString();
        BatchProgress progress = new BatchProgress(batchId);
        progressStore.put(batchId, progress);

        try {
            byte[] excelBytes = excelFile.getBytes();
            byte[] signatureImageBytes = signatureImage.getBytes();
            byte[] userCertificateBytes = userCertificate.getBytes();

            CompletableFuture.runAsync(() -> processBatch(
                    batchId,
                    templateId,
                    orgId,
                    excelBytes,
                    signatureImageBytes,
                    userCertificateBytes,
                        certificatePassword,
                        operatorName
            ));

            return new TemplateBatchStartResponse(
                    batchId,
                    STATUS_RUNNING,
                    "Batch generation started"
            );
        } catch (Exception e) {
            progress.markFailed("Cannot read uploaded files: " + e.getMessage());
            throw new RuntimeException("Cannot start batch generation", e);
        }
    }

    public TemplateBatchProgressResponse getProgress(String batchId) {
        BatchProgress progress = progressStore.get(batchId);
        if (progress == null) {
            throw new IllegalArgumentException("Batch not found: " + batchId);
        }

        return new TemplateBatchProgressResponse(
                progress.batchId,
                progress.status,
                progress.totalRows,
                progress.processedRows,
                progress.successCount,
                progress.failureCount,
                progress.progressPercent,
                progress.currentMessage,
                progress.startedAt,
                progress.finishedAt,
                List.copyOf(progress.errors)
        );
    }

    private void processBatch(String batchId,
                              String templateId,
                              Long orgId,
                              byte[] excelBytes,
                              byte[] signatureImageBytes,
                              byte[] userCertificateBytes,
                              String certificatePassword,
                              String operatorName) {
        BatchProgress progress = progressStore.get(batchId);
        if (progress == null) {
            return;
        }

        try {
            TemplateDocument template = templateService.getByIdForOrganization(templateId, orgId);
            Organization organization = organizationRepository.findById(orgId)
                    .orElseThrow(() -> new EntityNotFoundException("Organization not found"));

            List<TemplateField> fields = Optional.ofNullable(template.getFields()).orElseGet(ArrayList::new);
            if (fields.isEmpty()) {
                throw new IllegalArgumentException("Template schema is empty");
            }

            boolean hasSignatureField = fields.stream().anyMatch(f ->
                    "image".equalsIgnoreCase(f.getType()) && "signature".equalsIgnoreCase(f.getName()));
            if (!hasSignatureField) {
                throw new IllegalArgumentException("Template schema must include image field 'signature'");
            }

            if (!StringUtils.hasText(template.getPdfStorageKey())) {
                throw new IllegalArgumentException("Template PDF key is missing");
            }

            byte[] templatePdfBytes = s3Service.downloadFileAsBytes(template.getPdfStorageKey());
            ExcelData excelData = readExcel(excelBytes);
            validateSchemaAgainstExcel(fields, excelData.headers);

            progress.totalRows = excelData.rows.size();
            progress.currentMessage = "Processing certificates";
            progress.recalculatePercent();

            for (int i = 0; i < excelData.rows.size(); i++) {
                int rowNumber = i + 2;
                Map<String, String> row = excelData.rows.get(i);
                try {
                    String certificateId = requiredValue(row, "certificateid", rowNumber);
                    if (certificateRepository.existsByCertificateId(certificateId)) {
                        throw new IllegalArgumentException("certificateId already exists: " + certificateId);
                    }

                    byte[] unsignedPdf = pdfService.renderTemplatePdf(
                            templatePdfBytes,
                            fields,
                            row,
                            signatureImageBytes
                    );
                    log.debug("📋 Rendered unsigned PDF for {}: {} bytes", certificateId, unsignedPdf.length);

                    byte[] signedPdf = digitalSignatureService.signPdfWithUserCertificate(
                            unsignedPdf,
                            userCertificateBytes,
                            certificatePassword
                    );
                    log.debug("✍️ Signed PDF for {}: {} bytes", certificateId, signedPdf.length);

                    String signedHash = hashService.hashBytes(signedPdf);
                    log.info("🔐 Certificate {}: Hash of signed PDF = {}", certificateId, signedHash);

                    String folderName = "certificates/" + LocalDate.now().getYear();
                    String filename = certificateId + ".pdf";

                    String s3Url = s3Service.uploadFile(
                            new MockMultipartFile(filename, filename, "application/pdf", signedPdf),
                            folderName,
                            true,
                            10 * 1024 * 1024L
                    );
                    String s3Key = s3Url.replace(s3Domain + "/", "");
                    log.debug("☁️ Uploaded {} to S3: {}", certificateId, s3Url);

                    // Verify hash after S3 upload by re-downloading
                    try {
                        byte[] downloadedFromS3 = s3Service.downloadFileAsBytes(s3Key);
                        String verifyHash = hashService.hashBytes(downloadedFromS3);
                        if (verifyHash.equals(signedHash)) {
                            log.debug("✅ S3 upload verified - hash match: {}", verifyHash);
                        } else {
                            log.warn("⚠️ S3 upload hash mismatch for {}! Expected: {} but S3 has: {}", 
                                certificateId, signedHash, verifyHash);
                        }
                    } catch (Exception e) {
                        log.warn("Could not verify S3 upload hash for {}", certificateId, e);
                    }

                    log.info("📝 Issuing to blockchain - Certificate: {}, Hash: {}", certificateId, signedHash);
                    TransactionReceipt receipt = blockchainService.issueCertificate(certificateId, signedHash);
                    log.info("✅ Blockchain issued for {} - TxHash: {}", certificateId, receipt.getTransactionHash());

                    Certificate certificate = Certificate.builder()
                            .certificateId(certificateId)
                                .studentName(requiredValue(row, "studentname", rowNumber))
                                .dateOfBirth(optionalDate(row.get("dateofbirth")))
                                .major(optionalString(row.get("major")))
                                .graduationYear(optionalInteger(row.get("graduationyear")))
                                .gpa(optionalDouble(row.get("gpa")))
                                .certificateType(optionalString(row.get("certificatetype")) != null
                                    ? row.get("certificatetype") : template.getName())
                            .issuer(organization.getCode())
                                .issueDate(optionalDate(row.get("issuedate")) != null
                                    ? optionalDate(row.get("issuedate")) : LocalDate.now())
                            .pdfFilename(filename)
                            .pdfS3Path(s3Key)
                            .signedPdfS3Path(s3Key)
                            .pdfS3Url(s3Url)
                            .pdfSizeBytes((long) signedPdf.length)
                            .signedPdfHash(signedHash)
                            .signatureTimestamp(LocalDateTime.now())
                            .signerName(operatorName)
                            .isValid(true)
                            .status(CertificateStatus.SIGNED)
                            .claimCode(generateClaimCode(organization.getCode()))
                            .claimCodeExpiresAt(LocalDateTime.now().plusDays(30))
                            .isClaim(false)
                            .blockchainTxHash(receipt.getTransactionHash())
                            .blockchainBlockNumber(receipt.getBlockNumber().longValue())
                            .build();

                    try {
                        var block = blockchainService.getWeb3j()
                                .ethGetBlockByNumber(DefaultBlockParameter.valueOf(receipt.getBlockNumber()), false)
                                .send();
                        if (block.getBlock() != null) {
                            certificate.setBlockchainTimestamp(block.getBlock().getTimestamp().longValue());
                        }
                    } catch (Exception ignored) {
                        log.warn("Could not read blockchain block timestamp for {}", certificateId);
                    }

                    certificateRepository.save(certificate);
                    progress.successCount++;
                } catch (Exception rowException) {
                    String certId = row.get("certificateid");
                    progress.errors.add(new TemplateBatchRowErrorResponse(
                            rowNumber,
                            certId,
                            rowException.getMessage()
                    ));
                    progress.failureCount++;
                } finally {
                    progress.processedRows++;
                    progress.recalculatePercent();
                }
            }

            progress.currentMessage = "Batch generation completed";
            progress.status = STATUS_COMPLETED;
            progress.finishedAt = LocalDateTime.now();
            progress.recalculatePercent();
        } catch (Exception e) {
            progress.markFailed(e.getMessage());
            log.error("Template batch generation failed", e);
        }
    }

    private void validateInput(Long orgId,
                               MultipartFile excelFile,
                               MultipartFile signatureImage,
                               MultipartFile userCertificate,
                               String certificatePassword) {
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }

        if (excelFile == null || excelFile.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }
        if (excelFile.getSize() > MAX_EXCEL_SIZE) {
            throw new IllegalArgumentException("Excel file exceeds max size");
        }

        String excelName = Optional.ofNullable(excelFile.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!excelName.endsWith(".xlsx") && !excelName.endsWith(".xls")) {
            throw new IllegalArgumentException("Excel file must be .xlsx or .xls");
        }

        if (signatureImage == null || signatureImage.isEmpty()) {
            throw new IllegalArgumentException("Signature image is required");
        }
        if (signatureImage.getSize() > MAX_SIGNATURE_IMAGE_SIZE) {
            throw new IllegalArgumentException("Signature image exceeds max size");
        }

        if (userCertificate == null || userCertificate.isEmpty()) {
            throw new IllegalArgumentException("Digital certificate file is required");
        }
        if (userCertificate.getSize() > MAX_USER_CERT_SIZE) {
            throw new IllegalArgumentException("Digital certificate file exceeds max size");
        }

        String certName = Optional.ofNullable(userCertificate.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!certName.endsWith(".p12") && !certName.endsWith(".pfx")) {
            throw new IllegalArgumentException("Digital certificate must be .p12 or .pfx");
        }

        if (!StringUtils.hasText(certificatePassword)) {
            throw new IllegalArgumentException("certificatePassword is required");
        }
    }

    private void validateOrganizationPermission(Long orgId) {
        User user = currentUserProvider.get();
        Collection<OrganizationRole> allowedRoles = List.of(OrganizationRole.OWNER, OrganizationRole.MODERATOR);

        boolean hasPermission = organizationMemberRepository.existsByOrganization_IdAndUser_IdAndOrganizationRoleIn(
                orgId,
                user.getId(),
                allowedRoles
        );

        if (!hasPermission) {
            throw new AccessDeniedException("You are not allowed to generate certificates for this organization");
        }
    }

    private ExcelData readExcel(byte[] excelBytes) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() <= 1) {
                throw new IllegalArgumentException("Excel file does not contain data rows");
            }

            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel header row is missing");
            }

            List<String> headers = new ArrayList<>();
            int lastCell = headerRow.getLastCellNum();
            for (int col = 0; col < lastCell; col++) {
                String header = formatter.formatCellValue(headerRow.getCell(col));
                if (!StringUtils.hasText(header)) {
                    headers.add(("_col_" + col).toLowerCase(Locale.ROOT));
                } else {
                    headers.add(header.trim().toLowerCase(Locale.ROOT));
                }
            }

            Set<String> normalizedHeader = new HashSet<>();
            for (String header : headers) {
                String key = header.toLowerCase(Locale.ROOT);
                if (!normalizedHeader.add(key)) {
                    throw new IllegalArgumentException("Duplicated Excel header: " + header);
                }
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                Map<String, String> values = new HashMap<>();
                for (int col = 0; col < headers.size(); col++) {
                    String value = readCellValue(row.getCell(col), formatter);
                    values.put(headers.get(col), value);
                }
                rows.add(values);
            }

            if (rows.isEmpty()) {
                throw new IllegalArgumentException("Excel file does not contain valid data rows");
            }

            return new ExcelData(headers, rows);
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && StringUtils.hasText(new DataFormatter().formatCellValue(cell))) {
                return false;
            }
        }
        return true;
    }

    private String readCellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }

        CellType effectiveType = cell.getCellType();
        if (effectiveType == CellType.FORMULA) {
            effectiveType = cell.getCachedFormulaResultType();
        }

        // DateUtil should only be evaluated for numeric/date-capable cells.
        if (effectiveType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            LocalDate date = cell.getDateCellValue()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            return date.toString();
        }

        return formatter.formatCellValue(cell).trim();
    }

    private void validateSchemaAgainstExcel(List<TemplateField> fields, List<String> headers) {
        Set<String> normalizedHeaders = new HashSet<>();
        headers.forEach(h -> normalizedHeaders.add(h.toLowerCase(Locale.ROOT)));

        for (TemplateField field : fields) {
            if (field == null || !StringUtils.hasText(field.getName())) {
                continue;
            }
            if ("image".equalsIgnoreCase(field.getType())) {
                continue;
            }

            if (!normalizedHeaders.contains(field.getName().trim().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Excel does not contain required column: " + field.getName());
            }
        }

        List<String> requiredForCertificate = Arrays.asList("certificateid", "studentname");
        for (String requiredColumn : requiredForCertificate) {
            if (!normalizedHeaders.contains(requiredColumn.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Excel must contain column: " + requiredColumn);
            }
        }
    }

    private String requiredValue(Map<String, String> row, String key, int rowNumber) {
        String value = row.get(key);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Row " + rowNumber + " missing required value: " + key);
        }
        return value.trim();
    }

    private String optionalString(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Integer optionalInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid integer value: " + value);
        }
    }

    private Double optionalDouble(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid number value: " + value);
        }
    }

    private LocalDate optionalDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException("Invalid date value: " + value);
    }

    private String generateClaimCode(String issuer) {
        String code;
        do {
            int random = ThreadLocalRandom.current().nextInt(100000, 999999);
            String prefix = issuer == null || issuer.isBlank()
                    ? "ORG"
                    : issuer.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
            if (prefix.length() > 5) {
                prefix = prefix.substring(0, 5);
            }
            if (prefix.isBlank()) {
                prefix = "ORG";
            }
            code = prefix + "-" + random;
        } while (certificateRepository.existsByClaimCode(code));
        return code;
    }

    private record ExcelData(List<String> headers, List<Map<String, String>> rows) {
    }

    private static class BatchProgress {
        final String batchId;
        volatile String status;
        volatile Integer totalRows;
        volatile Integer processedRows;
        volatile Integer successCount;
        volatile Integer failureCount;
        volatile Integer progressPercent;
        volatile String currentMessage;
        final LocalDateTime startedAt;
        volatile LocalDateTime finishedAt;
        final List<TemplateBatchRowErrorResponse> errors;

        BatchProgress(String batchId) {
            this.batchId = batchId;
            this.status = STATUS_RUNNING;
            this.totalRows = 0;
            this.processedRows = 0;
            this.successCount = 0;
            this.failureCount = 0;
            this.progressPercent = 0;
            this.currentMessage = "Initializing";
            this.startedAt = LocalDateTime.now();
            this.finishedAt = null;
            this.errors = new ArrayList<>();
        }

        void recalculatePercent() {
            if (totalRows == null || totalRows <= 0) {
                progressPercent = 0;
                return;
            }

            int percent = (int) Math.round((processedRows * 100.0) / totalRows);
            if (STATUS_COMPLETED.equals(status) || STATUS_FAILED.equals(status)) {
                progressPercent = 100;
            } else {
                progressPercent = Math.min(percent, 99);
            }
        }

        void markFailed(String message) {
            this.status = STATUS_FAILED;
            this.currentMessage = message;
            this.finishedAt = LocalDateTime.now();
            this.recalculatePercent();
        }
    }
}
