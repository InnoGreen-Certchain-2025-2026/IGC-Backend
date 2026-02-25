package iuh.igc.service.core;

import iuh.igc.dto.request.core.CertificateRequest;
import iuh.igc.dto.response.core.CertificateResponse;
import iuh.igc.dto.response.core.VerifyResponse;
import iuh.igc.entity.Certificate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CertificateService {
    @Transactional
    CertificateResponse issueCertificate(CertificateRequest request, Long organizationId);

    VerifyResponse verifyCertificate(String certificateId);

    VerifyResponse verifyCertificateByFile(MultipartFile pdfFile);


    byte[] downloadCertificatePdf(String certificateId);

    @Transactional
    CertificateResponse revokeCertificate(String certificateId);

    @Transactional
    CertificateResponse reactivateCertificate(String certificateId);

    List<CertificateResponse> getAllCertificates();

    List<CertificateResponse> getAllCertificatesByStudentId();

    String generateClaimCode(String organizationCode);

    @Transactional
    CertificateResponse getCertificateByClaimCode(String claimCode);

    List<CertificateResponse> getCertificatesByOrganizationId(Long id);

    CertificateResponse getCertificateById(String certificateId);

    /**
     * Map Entity to Response
     */
    default CertificateResponse mapToResponse(Certificate certificate) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .certificateId(certificate.getCertificateId())
                .studentName(certificate.getStudentName())
                .studentId(certificate.getStudentId())
                .dateOfBirth(certificate.getDateOfBirth())
                .major(certificate.getMajor())
                .graduationYear(certificate.getGraduationYear())
                .gpa(certificate.getGpa())
                .certificateType(certificate.getCertificateType())
                .issuer(certificate.getIssuer())
                .issueDate(certificate.getIssueDate())
                .signedPdfHash(certificate.getSignedPdfHash())
                .blockchainTxHash(certificate.getBlockchainTxHash())
                .blockchainBlockNumber(certificate.getBlockchainBlockNumber())
                .blockchainTimestamp(certificate.getBlockchainTimestamp())
                .isValid(certificate.getIsValid())
                .createdAt(certificate.getCreatedAt())
                .build();
    }
}
