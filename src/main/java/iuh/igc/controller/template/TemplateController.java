package iuh.igc.controller.template;

import iuh.igc.dto.base.ApiResponse;
import iuh.igc.dto.request.template.CreateTemplateRequest;
import iuh.igc.dto.request.template.CreateTemplateWithSchemaRequest;
import iuh.igc.dto.request.template.SaveSchemaRequest;
import iuh.igc.dto.response.template.TemplateBatchProgressResponse;
import iuh.igc.dto.response.template.TemplateBatchStartResponse;
import iuh.igc.dto.response.template.TemplateResponse;
import iuh.igc.entity.template.TemplateDocument;
import iuh.igc.service.template.TemplateBatchCertificateService;
import iuh.igc.service.template.TemplateService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("api/templates")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateController {
    final TemplateService templateService;
    final TemplateBatchCertificateService templateBatchCertificateService;

    @GetMapping
    public ApiResponse<List<TemplateResponse>> getAll(
            @RequestParam Long orgId,
            @RequestParam(required = false, name = "q") String keyword) {
        List<TemplateResponse> data = templateService.getAll(orgId, keyword)
                .stream()
                .map(templateService::toResponse)
                .toList();
        return new ApiResponse<>(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<TemplateResponse> getById(
            @PathVariable String id,
            @RequestParam Long orgId) {
        return new ApiResponse<>(templateService.toResponse(templateService.getById(id, orgId)));
    }

    @PostMapping
    public ApiResponse<TemplateResponse> createLegacy(@RequestBody CreateTemplateRequest req) {
        TemplateDocument created = templateService.createTemplate(req.orgId(), req.name(), req.pdfStorageKey());
        return new ApiResponse<>(templateService.toResponse(created));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TemplateResponse> createWithPdf(
            @RequestPart("metadata") @Valid CreateTemplateWithSchemaRequest metadata,
            @RequestPart("pdfFile") MultipartFile pdfFile) {
        TemplateDocument created = templateService.createTemplateWithPdf(
                metadata.orgId(),
                metadata.name(),
                pdfFile,
                metadata.fields());
        return new ApiResponse<>(templateService.toResponse(created));
    }

    @PutMapping("/{id}")
    public ApiResponse<TemplateResponse> updateTemplateName(
            @PathVariable String id,
            @RequestParam Long orgId,
            @RequestParam String name) {
        TemplateDocument updated = templateService.updateTemplateName(id, orgId, name);
        return new ApiResponse<>(templateService.toResponse(updated));
    }

    @PutMapping("/{id}/schema")
    public ApiResponse<TemplateResponse> saveSchema(
            @PathVariable String id,
            @RequestBody @Valid SaveSchemaRequest req) {
        TemplateDocument updated = templateService.saveSchema(id, req.id(), req.fields());
        return new ApiResponse<>(templateService.toResponse(updated));
    }

    @PostMapping("/{id}/schema")
    public ApiResponse<TemplateResponse> createSchema(
            @PathVariable String id,
            @RequestBody @Valid SaveSchemaRequest req) {
        TemplateDocument updated = templateService.saveSchema(id, req.id(), req.fields());
        return new ApiResponse<>(templateService.toResponse(updated));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getTemplatePdf(
            @PathVariable String id,
            @RequestParam Long orgId) {
        byte[] pdfBytes = templateService.downloadTemplatePdf(id, orgId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=template-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/{id}/excel-template")
    public ResponseEntity<byte[]> downloadExcelTemplate(
            @PathVariable String id,
            @RequestParam Long orgId) {
        byte[] excelBytes = templateService.generateExcelTemplate(id, orgId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template-" + id + "-sample.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteTemplate(
            @PathVariable String id,
            @RequestParam Long orgId) {
        templateService.deleteTemplate(id, orgId);
        return new ApiResponse<>("Template deleted successfully");
    }

    @PostMapping(value = "/{id}/bulk-certificates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TemplateBatchStartResponse> bulkGenerateCertificates(
            @PathVariable String id,
            @RequestParam Long orgId,
            @RequestPart("excelFile") MultipartFile excelFile,
            @RequestPart("userCertificate") MultipartFile userCertificate,
            @RequestParam("certificatePassword") String certificatePassword) {
        TemplateBatchStartResponse response = templateBatchCertificateService.startBatchGeneration(
                id,
                orgId,
                excelFile,
                userCertificate,
                certificatePassword
        );
        return new ApiResponse<>(response);
    }

    @GetMapping("/batches/{batchId}/progress")
    public ApiResponse<TemplateBatchProgressResponse> getBatchProgress(@PathVariable String batchId) {
        return new ApiResponse<>(templateBatchCertificateService.getProgress(batchId));
    }
}
