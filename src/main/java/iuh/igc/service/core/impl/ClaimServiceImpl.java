package iuh.igc.service.core.impl;

import iuh.igc.advice.exception.InvalidClaimCodeException;
import iuh.igc.config.s3.S3Service;
import iuh.igc.dto.response.core.CertificateDownloadResponse;
import iuh.igc.dto.response.core.CertificateResponse;
import iuh.igc.entity.Certificate;
import iuh.igc.entity.User;
import iuh.igc.entity.constant.CertificateStatus;
import iuh.igc.repository.CertificateRepository;
import iuh.igc.service.core.CertificateMapper;
import iuh.igc.service.core.ClaimService;
import iuh.igc.service.user.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private final CertificateRepository certificateRepository;
    private final CertificateMapper certificateMapper;
    private final S3Service s3Service;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public CertificateResponse claimCertificate(String claimCode) {
        User currentUser = currentUserProvider.get();
        Certificate certificate = findValidClaimCertificate(claimCode);

        if (Boolean.TRUE.equals(certificate.getIsClaim())) {
            if (currentUser.getId().equals(certificate.getStudentId())) {
                return certificateMapper.toResponse(certificate);
            }
            throw new InvalidClaimCodeException("Certificate has already been claimed");
        }

        certificate.setStudentId(currentUser.getId());
        certificate.setIsClaim(true);
        Certificate saved = certificateRepository.save(certificate);

        log.info("Certificate {} claimed by user {}", saved.getCertificateId(), currentUser.getId());
        return certificateMapper.toResponse(saved);
    }

    @Override
    public CertificateResponse getCertificateByClaimCode(String claimCode) {
        Certificate certificate = findValidClaimCertificate(claimCode);
        log.info("Claim lookup success for code {}", claimCode);
        return certificateMapper.toResponse(certificate);
    }

    @Override
    public CertificateDownloadResponse downloadCertificateByClaimCode(String claimCode) {
        Certificate certificate = findValidClaimCertificate(claimCode);

        String signedPath = certificate.getSignedPdfS3Path();
        if (signedPath == null || signedPath.isBlank()) {
            throw new InvalidClaimCodeException("Signed PDF not found for claim code");
        }

        byte[] bytes = s3Service.downloadFileAsBytes(signedPath);
        String filename = certificate.getCertificateId() + "-signed.pdf";

        return new CertificateDownloadResponse(filename, bytes);
    }

    private Certificate findValidClaimCertificate(String claimCode) {
        Certificate certificate = certificateRepository.findByClaimCodeAndStatus(claimCode, CertificateStatus.SIGNED)
                .orElseThrow(() -> new InvalidClaimCodeException("Invalid claim code"));

        if (!Boolean.TRUE.equals(certificate.getIsValid())) {
            throw new InvalidClaimCodeException("Certificate has been revoked");
        }
        if (certificate.getClaimCodeExpiresAt() != null
                && LocalDateTime.now().isAfter(certificate.getClaimCodeExpiresAt())) {
            throw new InvalidClaimCodeException("Claim code has expired");
        }
        return certificate;
    }
}
