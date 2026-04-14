package iuh.igc.controller;

import iuh.igc.dto.base.ApiResponse;
import iuh.igc.dto.request.core.CreateDraftRequest;
import iuh.igc.dto.request.core.CertificateRequest;
import iuh.igc.dto.request.core.SignCertificateRequest;
import iuh.igc.dto.request.core.SignaturePosition;
import iuh.igc.dto.response.core.CertificateDownloadResponse;
import iuh.igc.dto.response.core.CertificateResponse;
import iuh.igc.dto.response.core.VerifyResponse;
import iuh.igc.service.core.CertificateService;
import iuh.igc.service.core.ClaimService;
import iuh.igc.service.core.DraftCertificateService;
import iuh.igc.service.core.SigningService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificateController {

    DraftCertificateService draftCertificateService;
    SigningService signingService;
    ClaimService claimService;
    CertificateService certificateService;

    @PostMapping("/draft")
    public ApiResponse<CertificateResponse> createDraftCertificate(
            @Valid @RequestBody CreateDraftRequest request
    ) {
        log.info("Create draft certificate request: {}", request.certificateId());
        return new ApiResponse<>(draftCertificateService.createDraft(request));
    }

    @GetMapping("/drafts")
    public ApiResponse<List<CertificateResponse>> getDraftCertificates() {
        return new ApiResponse<>(draftCertificateService.getDraftCertificates());
    }

    @GetMapping("/signed")
    public ApiResponse<List<CertificateResponse>> getSignedCertificates() {
        return new ApiResponse<>(draftCertificateService.getSignedCertificates());
    }

    @GetMapping("/revoked")
    public ApiResponse<List<CertificateResponse>> getRevokedCertificates() {
        return new ApiResponse<>(draftCertificateService.getRevokedCertificates());
    }

    @GetMapping("/my-certificates")
    public ApiResponse<List<CertificateResponse>> getMyCertificates() {
        return new ApiResponse<>(certificateService.getAllCertificatesByStudentId());
    }

    @PostMapping(value = "/issue", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CertificateResponse> issueCertificate(
            @RequestPart("request") @Valid CertificateRequest request,
            @RequestPart("userCertificate") MultipartFile userCertificate,
            @RequestParam("certificatePassword") String certificatePassword,
            @RequestParam("organizationId") Long organizationId
    ) {
        return new ApiResponse<>(certificateService.issueCertificate(
                request,
                organizationId,
                userCertificate,
                certificatePassword
        ));
    }

    @PostMapping(value = "/{certificateId}/sign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CertificateResponse> signCertificate(
            @PathVariable String certificateId,
            @RequestPart("signatureImage") MultipartFile signatureImage,
            @RequestPart("userCertificate") MultipartFile userCertificate,
            @RequestParam("certificatePassword") String certificatePassword,
            @RequestParam("x") Float x,
            @RequestParam("y") Float y,
            @RequestParam("width") Float width,
            @RequestParam("height") Float height
    ) {
        SignCertificateRequest request = new SignCertificateRequest(
                signatureImage,
                userCertificate,
                certificatePassword,
                new SignaturePosition(x, y, width, height)
        );
        return new ApiResponse<>(signingService.signCertificate(certificateId, request));
    }

    @GetMapping("/{certificateId}/verify")
    public ApiResponse<VerifyResponse> verifyCertificate(@PathVariable String certificateId) {
        return new ApiResponse<>(certificateService.verifyCertificate(certificateId));
    }

    @PostMapping(value = "/verify/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VerifyResponse> verifyCertificateByFile(
            @RequestPart("pdfFile") MultipartFile pdfFile
    ) {
        return new ApiResponse<>(certificateService.verifyCertificateByFile(pdfFile));
    }

    @DeleteMapping("/{certificateId}/revoke")
    public ApiResponse<CertificateResponse> revokeCertificate(
            @PathVariable String certificateId
    ) {
        return new ApiResponse<>(signingService.revokeCertificate(certificateId));
    }

    @PostMapping("/{certificateId}/reissue")
    public ApiResponse<CertificateResponse> reissueCertificate(
            @PathVariable String certificateId
    ) {
        return new ApiResponse<>(draftCertificateService.reissueCertificate(certificateId));
    }

    @PostMapping("/claim/{claimCode}")
    public ApiResponse<CertificateResponse> claimCertificateOwnership(@PathVariable String claimCode) {
        return new ApiResponse<>(claimService.claimCertificate(claimCode));
    }

    @GetMapping("/claim/{claimCode}")
    public ApiResponse<CertificateResponse> claimCertificate(@PathVariable String claimCode) {
        return new ApiResponse<>(claimService.getCertificateByClaimCode(claimCode));
    }

    @GetMapping("/claim/{claimCode}/download")
    public ResponseEntity<byte[]> downloadClaimCertificate(
            @PathVariable String claimCode
    ) {
        CertificateDownloadResponse response = claimService.downloadCertificateByClaimCode(claimCode);

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"" + response.filename() + "\"")
                .header("Content-Type", "application/pdf")
                .body(response.bytes());
    }

            @GetMapping("/{certificateId}/download")
            public ResponseEntity<byte[]> downloadMyCertificate(
                @PathVariable String certificateId
            ) {
            CertificateDownloadResponse response = certificateService.downloadCertificate(certificateId);

            return ResponseEntity.ok()
                .header("Content-Disposition",
                    "attachment; filename=\"" + response.filename() + "\"")
                .header("Content-Type", "application/pdf")
                .body(response.bytes());
            }
}