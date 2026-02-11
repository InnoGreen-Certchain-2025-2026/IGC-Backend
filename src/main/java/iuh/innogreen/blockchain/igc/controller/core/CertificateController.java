package iuh.innogreen.blockchain.igc.controller.core;

import iuh.innogreen.blockchain.igc.dto.request.CertificateRequest;
import iuh.innogreen.blockchain.igc.dto.response.CertificateResponse;
import iuh.innogreen.blockchain.igc.service.core.CertificateService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * Cấp chứng chỉ mới
     * POST /api/certificates
     */
    @PostMapping
    public ResponseEntity<?> issueCertificate(@Valid @RequestBody CertificateRequest request) {
        try {
            log.info("📥 Received request to issue certificate: {}", request.getCertificateId());
            CertificateResponse response = certificateService.issueCertificate(request);

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

    /**
     * Lấy tất cả chứng chỉ
     * GET /api/certificates
     */
    @GetMapping
    public ResponseEntity<?> getAllCertificates() {
        try {
            List<CertificateResponse> certificates = certificateService.getAllCertificates();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", certificates.size());
            result.put("data", certificates);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error getting certificates", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to retrieve certificates");
            error.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Lấy chứng chỉ theo ID
     * GET /api/certificates/{certificateId}
     */
    @GetMapping("/{certificateId}")
    public ResponseEntity<?> getCertificateById(@PathVariable String certificateId) {
        try {
            CertificateResponse certificate = certificateService.getCertificateById(certificateId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", certificate);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error getting certificate", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Certificate not found");
            error.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Thu hồi chứng chỉ
     * DELETE /api/certificates/{certificateId}
     */
    @DeleteMapping("/{certificateId}")
    public ResponseEntity<?> revokeCertificate(@PathVariable String certificateId) {
        try {
            log.info("📥 Received request to revoke certificate: {}", certificateId);
            CertificateResponse response = certificateService.revokeCertificate(certificateId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Certificate revoked successfully");
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error revoking certificate", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to revoke certificate");
            error.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
