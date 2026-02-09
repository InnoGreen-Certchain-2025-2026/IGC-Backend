package iuh.innogreen.blockchain.igc.controller;

import iuh.innogreen.blockchain.igc.dto.response.VerifyResponse;
import iuh.innogreen.blockchain.igc.service.CertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/verify")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VerifyController {

    private final CertificateService certificateService;

    /**
     * Xác thực chứng chỉ
     * GET /api/verify/{certificateId}
     */
    @GetMapping("/{certificateId}")
    public ResponseEntity<?> verifyCertificate(@PathVariable String certificateId) {
        try {
            log.info("🔍 Verifying certificate: {}", certificateId);
            VerifyResponse response = certificateService.verifyCertificate(certificateId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error verifying certificate", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Verification failed");
            error.put("error", e.getMessage());

            return ResponseEntity.status(500).body(error);
        }
    }
}
