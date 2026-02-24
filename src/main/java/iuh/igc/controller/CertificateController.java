package iuh.igc.controller;

import iuh.igc.dto.request.core.CertificateRequest;
import iuh.igc.dto.response.core.CertificateResponse;
import iuh.igc.dto.response.core.VerifyResponse;
import iuh.igc.service.core.CertificateService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificateController {

    CertificateService certificateService;

    /**
     * ===============================
     * ISSUE CERTIFICATE
     * POST /api/certificates
     * ===============================
     */
    @PostMapping
    public ResponseEntity<?> issueCertificate(
            @Valid @RequestBody CertificateRequest request,
            Authentication authentication
    ) {
        try {
            String vendorUsername = authentication.getName();

            log.info("📥 Request to issue certificate: {}", request.certificateId());

            CertificateResponse response =
                    certificateService.issueCertificate(request, vendorUsername);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Certificate issued successfully");
            result.put("data", response);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (Exception e) {
            log.error("❌ Error issuing certificate", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to issue certificate");
            error.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping(value = "/by-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> verifyCertificateByFile(@RequestParam("file") MultipartFile file) {
        try {
            log.info("🔍 ========== VERIFICATION REQUEST BY FILE ==========");
            log.info("File name: {}", file.getOriginalFilename());
            log.info("File size: {} bytes", file.getSize());
            log.info("Content type: {}", file.getContentType());

            VerifyResponse response = certificateService.verifyCertificateByFile(file);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);

            Map<String, Object> summary = new HashMap<>();
            summary.put("certificateExists", response.exists());
            summary.put("isValid", response.valid());
            summary.put("fileAuthentic", response.valid());
            summary.put("allChecksPass", response.exists() && response.valid());
            result.put("summary", summary);

            log.info("Verification result: {}", response.valid() ? "✅ AUTHENTIC" : "❌ INVALID/TAMPERED");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ File verification error", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "File verification failed");
            error.put("error", e.getMessage());

            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * ===============================
     * VERIFY CERTIFICATE
     * GET /api/certificates/{id}/verify
     * ===============================
     */
//    @GetMapping("/{certificateId}/verify")
//    public ResponseEntity<?> verifyCertificate(@PathVariable String certificateId) {
//        try {
//            VerifyResponse response =
//                    certificateService.verifyCertificate(certificateId);
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("success", response.valid());
//            result.put("data", response);
//
//            return ResponseEntity.ok(result);
//
//        } catch (Exception e) {
//            log.error("❌ Error verifying certificate", e);
//
//            Map<String, Object> error = new HashMap<>();
//            error.put("success", false);
//            error.put("message", "Verification failed");
//            error.put("error", e.getMessage());
//
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
//        }
//    }
//
//    /**
//     * ===============================
//     * DOWNLOAD PDF
//     * GET /api/certificates/{id}/download
//     * ===============================
//     */
//    @GetMapping("/{certificateId}/download")
//    public ResponseEntity<?> downloadCertificate(@PathVariable String certificateId) {
//        try {
//            byte[] pdfBytes =
//                    certificateService.downloadCertificatePdf(certificateId);
//
//            ByteArrayResource resource = new ByteArrayResource(pdfBytes);
//
//            return ResponseEntity.ok()
//                    .header(HttpHeaders.CONTENT_DISPOSITION,
//                            "attachment; filename=\"" + certificateId + ".pdf\"")
//                    .contentType(MediaType.APPLICATION_PDF)
//                    .contentLength(pdfBytes.length)
//                    .body(resource);
//
//        } catch (Exception e) {
//            log.error("❌ Error downloading certificate", e);
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body("Certificate not found or download failed");
//        }
//    }

    /**
     * ===============================
     * GET ALL CERTIFICATES
     * GET /api/certificates
     * ===============================
     */
    @GetMapping
    public ResponseEntity<?> getAllCertificates() {
        try {
            List<CertificateResponse> certificates =
                    certificateService.getAllCertificates();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", certificates.size());
            result.put("data", certificates);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error retrieving certificates", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve certificates");
            error.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * ===============================
     * GET CERTIFICATE BY ID
     * GET /api/certificates/{id}
     * ===============================
     */
    @GetMapping("/{certificateId}")
    public ResponseEntity<?> getCertificateById(@PathVariable String certificateId) {
        try {
            CertificateResponse certificate =
                    certificateService.getCertificateById(certificateId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", certificate);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Certificate not found", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Certificate not found");
            error.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * ===============================
     * REVOKE CERTIFICATE
     * DELETE /api/certificates/{id}
     * ===============================
     */
//    @DeleteMapping("/{certificateId}")
//    public ResponseEntity<?> revokeCertificate(@PathVariable String certificateId) {
//        try {
//            CertificateResponse response =
//                    certificateService.revokeCertificate(certificateId);
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("success", true);
//            result.put("message", "Certificate revoked successfully");
//            result.put("data", response);
//
//            return ResponseEntity.ok(result);
//
//        } catch (Exception e) {
//            log.error("❌ Error revoking certificate", e);
//
//            Map<String, Object> error = new HashMap<>();
//            error.put("success", false);
//            error.put("message", "Failed to revoke certificate");
//            error.put("error", e.getMessage());
//
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
//        }
//    }
}