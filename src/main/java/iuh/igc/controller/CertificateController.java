package iuh.igc.controller;

import iuh.igc.dto.base.ApiResponse;
import iuh.igc.dto.request.core.CertificateRequest;
import iuh.igc.dto.response.core.CertificateDownloadResponse;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificateController {

    CertificateService certificateService;

    /**
     * Issue new certificate
     * POST /api/certificates
     */
    @PostMapping("/{id}")
    public ApiResponse<CertificateResponse> issueCertificate(
            @Valid @RequestBody CertificateRequest request, @PathVariable("id") Long id
    ) {
        try {

            log.info("Request to issue certificate: {} by user: {}", request.certificateId(), id);

            CertificateResponse response = certificateService.issueCertificate(request, id);

            return new ApiResponse<>(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid certificate data: {}", e.getMessage());
            return new ApiResponse<>(e.getMessage(), HttpStatus.BAD_REQUEST.value());

        } catch (Exception e) {
            log.error("Failed to issue certificate", e);
            return new ApiResponse<>("Failed to issue certificate", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    /**
     * Verify certificate by file upload
     * POST /api/certificates/verify/by-file
     */
    @PostMapping(value = "/verify/by-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VerifyResponse> verifyCertificateByFile(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            log.info("Verification request by file: {} - {} bytes",
                    file.getOriginalFilename(), file.getSize());

            VerifyResponse response = certificateService.verifyCertificateByFile(file);

            log.info("Verification result: {}", response.valid() ? "VALID" : "INVALID");

            return new ApiResponse<>(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid file: {}", e.getMessage());
            return new ApiResponse<>(e.getMessage(), HttpStatus.BAD_REQUEST.value());

        } catch (Exception e) {
            log.error("File verification failed", e);
            return new ApiResponse<>("File verification failed", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    /**
     * Verify certificate by certificate ID
     * GET /api/certificates/{certificateId}/verify
     */
    @GetMapping("/{certificateId}/verify")
    public ApiResponse<VerifyResponse> verifyCertificate(
            @PathVariable String certificateId
    ) {
        try {
            log.info("Verification request for certificate: {}", certificateId);

            VerifyResponse response = certificateService.verifyCertificate(certificateId);

            return new ApiResponse<>(response);

        } catch (Exception e) {
            log.error("Verification failed for certificate: {}", certificateId, e);
            return new ApiResponse<>("Verification failed", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    /**
     * Get all certificates
     * GET /api/certificates
     */
    @GetMapping
    public ApiResponse<List<CertificateResponse>> getAllCertificates() {
        try {
            log.info("Request to get all certificates");

            List<CertificateResponse> certificates = certificateService.getAllCertificates();

            return new ApiResponse<>(certificates);

        } catch (Exception e) {
            log.error("Failed to retrieve certificates", e);
            return new ApiResponse<>("Failed to retrieve certificates", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @GetMapping("/organization/{id}")
    public ApiResponse<List<CertificateResponse>> getCertificatesByOrganizationId(@PathVariable("id") Long id) {
        try {
            log.info("Request to get certificates for organization: {}", id);

            List<CertificateResponse> certificates = certificateService.getCertificatesByOrganizationId(id);

            return new ApiResponse<>(certificates);

        } catch (Exception e) {
            log.error("Failed to retrieve certificates for organization: {}", id, e);
            return new ApiResponse<>("Failed to retrieve certificates", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    /**
     * Get certificate by ID
     * GET /api/certificates/{certificateId}
     */
    @GetMapping("/{certificateId}")
    public ApiResponse<CertificateResponse> getCertificateById(
            @PathVariable String certificateId
    ) {
        try {
            log.info("Request to get certificate: {}", certificateId);

            CertificateResponse certificate = certificateService.getCertificateById(certificateId);

            return new ApiResponse<>(certificate);

        } catch (IllegalArgumentException e) {
            log.error("Certificate not found: {}", certificateId);
            return new ApiResponse<>(e.getMessage(), HttpStatus.NOT_FOUND.value());

        } catch (Exception e) {
            log.error("Failed to get certificate: {}", certificateId, e);
            return new ApiResponse<>("Failed to get certificate", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    /**
     * Revoke certificate
     * DELETE /api/certificates/{certificateId}
     */
    @DeleteMapping("/{certificateId}")
    public ApiResponse<CertificateResponse> revokeCertificate(
            @PathVariable String certificateId
    ) {
        try {
            log.info("Request to revoke certificate: {}", certificateId);

            CertificateResponse response = certificateService.revokeCertificate(certificateId);

            return new ApiResponse<>(response);

        } catch (IllegalArgumentException e) {
            log.error("Certificate not found: {}", certificateId);
            return new ApiResponse<>(e.getMessage(), HttpStatus.NOT_FOUND.value());

        } catch (Exception e) {
            log.error("Failed to revoke certificate: {}", certificateId, e);
            return new ApiResponse<>("Failed to revoke certificate", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    /**
     * Reactivate revoked certificate
     * POST /api/certificates/{certificateId}/reactivate
     */
    @PostMapping("/{certificateId}/reactivate")
    public ApiResponse<CertificateResponse> reactivateCertificate(
            @PathVariable String certificateId
    ) {
        try {
            log.info("Request to reactivate certificate: {}", certificateId);

            CertificateResponse response = certificateService.reactivateCertificate(certificateId);

            return new ApiResponse<>(response);

        } catch (IllegalArgumentException e) {
            log.error("Certificate not found or already active: {}", certificateId);
            return new ApiResponse<>(e.getMessage(), HttpStatus.BAD_REQUEST.value());

        } catch (Exception e) {
            log.error("Failed to reactivate certificate: {}", certificateId, e);
            return new ApiResponse<>("Failed to reactivate certificate", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @PostMapping("/claim")
    public ApiResponse<CertificateResponse> claimCertificate(
            @RequestParam String claimCode
    ) {
        log.info("📥 Claim request - code: {}", claimCode);

        CertificateResponse response =
                certificateService.getCertificateByClaimCode(claimCode);

        return new ApiResponse<>(response);
    }


    @GetMapping("/my-certificates")
    public ApiResponse<List<CertificateResponse>> getMyCertificates() {

        List<CertificateResponse> responses =
                certificateService.getAllCertificatesByStudentId();

        return new ApiResponse<>(responses);
    }

    @GetMapping("/{certificateId}/download")
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable String certificateId
    ) {
        CertificateDownloadResponse response =
                certificateService.downloadCertificate(certificateId);

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"" + response.filename() + "\"")
                .header("Content-Type", "application/pdf")
                .body(response.bytes());
    }
}