package iuh.igc.service.core.impl;

import iuh.igc.advice.exception.CertificateAlreadySignedException;
import iuh.igc.advice.exception.CertificateNotFoundException;
import iuh.igc.advice.exception.InvalidCertificateException;
import iuh.igc.config.s3.S3Service;
import iuh.igc.dto.request.core.SignCertificateRequest;
import iuh.igc.dto.response.core.CertificateResponse;
import iuh.igc.entity.Certificate;
import iuh.igc.entity.User;
import iuh.igc.entity.constant.CertificateStatus;
import iuh.igc.entity.constant.OrganizationRole;
import iuh.igc.entity.organization.Organization;
import iuh.igc.repository.CertificateRepository;
import iuh.igc.repository.OrganizationMemberRepository;
import iuh.igc.repository.OrganizationRepository;
import iuh.igc.service.core.BlockchainService;
import iuh.igc.service.core.CertificateMapper;
import iuh.igc.service.core.SigningService;
import iuh.igc.service.pdf.DigitalSignatureService;
import iuh.igc.service.pdf.HashService;
import iuh.igc.service.pdf.PdfService;
import iuh.igc.service.user.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class SigningServiceImpl implements SigningService {

    private static final long MAX_SIGNATURE_IMAGE_SIZE = 3 * 1024 * 1024;
    private static final long MAX_CERTIFICATE_SIZE = 5 * 1024 * 1024;

    private final CertificateRepository certificateRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PdfService pdfService;
    private final DigitalSignatureService digitalSignatureService;
    private final HashService hashService;
    private final BlockchainService blockchainService;
    private final S3Service s3Service;
    private final CertificateMapper certificateMapper;

    @Value("${aws.s3.domain}")
    private String domain;

    @Override
    public CertificateResponse signCertificate(String certificateId, SignCertificateRequest request) {
        Certificate certificate = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found: " + certificateId));

        if (certificate.getStatus() == CertificateStatus.SIGNED) {
            throw new CertificateAlreadySignedException("Certificate already signed");
        }

        if (certificate.getStatus() != CertificateStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT certificates can be signed");
        }

        validateSignRequest(request);
        validateAuthorization(certificate);

        String draftKey = certificate.getDraftPdfS3Path();
        if (draftKey == null || draftKey.isBlank()) {
            throw new IllegalStateException("Draft PDF path is missing");
        }

        try {
            byte[] draftPdf = s3Service.downloadFileAsBytes(draftKey);
            byte[] pdfWithImage = pdfService.addSignatureImageToPdf(
                    draftPdf,
                    request.signatureImage().getBytes(),
                    request.signaturePosition()
            );

            byte[] signedPdf = digitalSignatureService.signPdfWithUserCertificate(
                    pdfWithImage,
                    request.userCertificate().getBytes(),
                    request.certificatePassword()
            );

            String signedHash = hashService.hashBytes(signedPdf);
            String signedFolder = "signed/" + certificate.getIssueDate().getYear();
            String signedFilename = certificate.getCertificateId() + "-signed.pdf";

            String signedUrl = s3Service.uploadFile(
                    new MockMultipartFile(signedFilename, signedFilename, "application/pdf", signedPdf),
                    signedFolder,
                    true,
                    10 * 1024 * 1024
            );
            String signedKey = signedUrl.replace(domain + "/", "");

            s3Service.deleteFileByKey(draftKey);

            TransactionReceipt receipt = blockchainService.issueCertificate(
                    certificate.getCertificateId(),
                    signedHash
            );

            certificate.setStatus(CertificateStatus.SIGNED);
            certificate.setSignedPdfHash(signedHash);
            certificate.setSignedPdfS3Path(signedKey);
            certificate.setDraftPdfS3Path(null);
            certificate.setPdfS3Path(signedKey);
            certificate.setPdfS3Url(signedUrl);
            certificate.setPdfFilename(signedFilename);
            certificate.setPdfSizeBytes((long) signedPdf.length);
            certificate.setClaimCode(generateClaimCode(certificate.getIssuer()));
            certificate.setClaimCodeExpiresAt(LocalDateTime.now().plusDays(30));
            certificate.setSignatureTimestamp(LocalDateTime.now());
            certificate.setSignerName(currentUserProvider.get().getName());
            certificate.setBlockchainTxHash(receipt.getTransactionHash());
            certificate.setBlockchainBlockNumber(receipt.getBlockNumber().longValue());
            certificate.setIsValid(true);

            try {
                var block = blockchainService.getWeb3j().ethGetBlockByNumber(
                        org.web3j.protocol.core.DefaultBlockParameter.valueOf(receipt.getBlockNumber()),
                        false
                ).send();
                if (block.getBlock() != null) {
                    certificate.setBlockchainTimestamp(block.getBlock().getTimestamp().longValue());
                }
            } catch (Exception ex) {
                log.warn("Unable to fetch block timestamp for tx {}", receipt.getTransactionHash(), ex);
            }

            Certificate saved = certificateRepository.save(certificate);
            log.info("Certificate {} signed successfully", certificateId);
            return certificateMapper.toResponse(saved);
        } catch (InvalidCertificateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to sign certificate {}", certificateId, e);
            throw new RuntimeException("Failed to sign certificate", e);
        }
    }

    @Override
    public CertificateResponse revokeCertificate(String certificateId) {
        Certificate certificate = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found: " + certificateId));

        if (certificate.getStatus() != CertificateStatus.SIGNED) {
            throw new IllegalStateException("Only SIGNED certificates can be revoked");
        }

        validateAuthorization(certificate);

        blockchainService.revokeCertificate(certificateId);

        certificate.setIsValid(false);
        certificate.setStatus(CertificateStatus.REVOKED);

        Certificate saved = certificateRepository.save(certificate);
        log.info("Certificate {} revoked successfully", certificateId);

        return certificateMapper.toResponse(saved);
    }

    private void validateSignRequest(SignCertificateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Sign request is required");
        }
        if (request.signatureImage() == null || request.signatureImage().isEmpty()) {
            throw new IllegalArgumentException("Signature image is required");
        }
        if (request.userCertificate() == null || request.userCertificate().isEmpty()) {
            throw new IllegalArgumentException("User certificate is required");
        }
        if (request.certificatePassword() == null || request.certificatePassword().isBlank()) {
            throw new IllegalArgumentException("Certificate password is required");
        }
        if (request.signaturePosition() == null) {
            throw new IllegalArgumentException("Signature position is required");
        }

        String contentType = request.signatureImage().getContentType();
        if (contentType == null ||
                !("image/png".equalsIgnoreCase(contentType)
                        || "image/jpeg".equalsIgnoreCase(contentType)
                        || "image/jpg".equalsIgnoreCase(contentType))) {
            throw new IllegalArgumentException("Signature image must be PNG or JPEG");
        }

        if (request.signatureImage().getSize() > MAX_SIGNATURE_IMAGE_SIZE) {
            throw new IllegalArgumentException("Signature image exceeds max size 3MB");
        }

        String certName = request.userCertificate().getOriginalFilename();
        if (certName == null ||
                !(certName.toLowerCase().endsWith(".p12") || certName.toLowerCase().endsWith(".pfx"))) {
            throw new IllegalArgumentException("User certificate must be a .p12 or .pfx file");
        }

        if (request.userCertificate().getSize() > MAX_CERTIFICATE_SIZE) {
            throw new IllegalArgumentException("User certificate exceeds max size 5MB");
        }
    }

    private void validateAuthorization(Certificate certificate) {
        User currentUser = currentUserProvider.get();

        boolean isAdmin = isAdmin();
        if (isAdmin) {
            return;
        }

        Organization organization = organizationRepository.findByCode(certificate.getIssuer())
                .orElseThrow(() -> new CertificateNotFoundException("Issuer organization not found"));

        boolean canManage = organizationMemberRepository.existsByOrganization_IdAndUser_IdAndOrganizationRoleIn(
                organization.getId(),
                currentUser.getId(),
                List.of(OrganizationRole.OWNER, OrganizationRole.MODERATOR)
        );

        if (!canManage) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only organization owner/moderator or admin can sign/revoke this certificate"
            );
        }
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_ADMIN".equalsIgnoreCase(authority)
                        || "ADMIN".equalsIgnoreCase(authority)
                        || "SCOPE_ADMIN".equalsIgnoreCase(authority));
    }

    private String generateClaimCode(String organizationCode) {
        String claimCode;
        do {
            int number = ThreadLocalRandom.current().nextInt(100000, 1000000);
            claimCode = organizationCode + "-" + number;
        } while (certificateRepository.existsByClaimCode(claimCode));
        return claimCode;
    }
}
