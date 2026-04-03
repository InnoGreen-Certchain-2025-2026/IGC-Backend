package iuh.igc.service.core;

import iuh.igc.dto.response.core.CertificateResponse;
import iuh.igc.entity.Certificate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificateMapper {

    @Value("${aws.s3.domain}")
    String s3Domain;

    public CertificateResponse toResponse(Certificate certificate) {
        String downloadUrl = null;
        if (certificate.getClaimCode() != null && certificate.getStatus() != null) {
            downloadUrl = "/api/certificates/claim/" + certificate.getClaimCode() + "/download";
        }

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
                .status(certificate.getStatus())
                .claimCode(certificate.getClaimCode())
                .claimCodeExpiresAt(certificate.getClaimCodeExpiresAt())
                .draftPdfS3Path(certificate.getDraftPdfS3Path())
                .signedPdfS3Path(certificate.getSignedPdfS3Path())
                .downloadUrl(downloadUrl)
                .createdAt(certificate.getCreatedAt())
                .build();
    }

    public String toSignedFileUrl(Certificate certificate) {
        if (certificate.getSignedPdfS3Path() == null) {
            return null;
        }
        return s3Domain + "/" + certificate.getSignedPdfS3Path();
    }
}
