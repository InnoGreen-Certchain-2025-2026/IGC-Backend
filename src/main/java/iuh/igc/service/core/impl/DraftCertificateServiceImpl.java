package iuh.igc.service.core.impl;

import iuh.igc.advice.exception.CertificateNotFoundException;
import iuh.igc.config.s3.S3Service;
import iuh.igc.dto.request.core.CertificateRequest;
import iuh.igc.dto.request.core.CreateDraftRequest;
import iuh.igc.dto.response.core.CertificateResponse;
import iuh.igc.entity.Certificate;
import iuh.igc.entity.User;
import iuh.igc.entity.constant.CertificateStatus;
import iuh.igc.entity.constant.OrganizationRole;
import iuh.igc.entity.organization.OrganizationMember;
import iuh.igc.entity.organization.Organization;
import iuh.igc.repository.CertificateRepository;
import iuh.igc.repository.OrganizationMemberRepository;
import iuh.igc.repository.OrganizationRepository;
import iuh.igc.service.core.CertificateMapper;
import iuh.igc.service.core.DraftCertificateService;
import iuh.igc.service.pdf.PdfService;
import iuh.igc.service.user.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftCertificateServiceImpl implements DraftCertificateService {

    private final CertificateRepository certificateRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PdfService pdfService;
    private final S3Service s3Service;
    private final CertificateMapper certificateMapper;

    @Value("${aws.s3.domain}")
    private String domain;

    @Override
        public CertificateResponse createDraft(CreateDraftRequest request) {
        User currentUser = currentUserProvider.get();

        List<OrganizationMember> manageableMemberships = organizationMemberRepository
            .findByUser_IdAndOrganizationRoleIn(
                currentUser.getId(),
                List.of(OrganizationRole.OWNER, OrganizationRole.MODERATOR)
            );

        if (manageableMemberships.isEmpty()) {
            throw new AccessDeniedException("You are not allowed to create draft certificates");
        }

        Organization organization = manageableMemberships.stream()
            .map(OrganizationMember::getOrganization)
            .sorted((a, b) -> a.getId().compareTo(b.getId()))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Organization not found"));

        if (manageableMemberships.size() > 1) {
            log.info("User {} belongs to {} manageable organizations. Using organization {} for draft {}",
                currentUser.getId(),
                manageableMemberships.size(),
                organization.getCode(),
                request.certificateId());
        }

        if (certificateRepository.existsByCertificateId(request.certificateId())) {
            throw new IllegalArgumentException("Certificate ID already exists: " + request.certificateId());
        }

        log.info("Creating draft certificate {} for organization {}", request.certificateId(), organization.getCode());

        byte[] draftPdf = pdfService.generateCertificatePdf(toLegacyRequest(request), organization.getName());
        String folderName = "drafts/" + request.issueDate().getYear();
        String fileName = request.certificateId() + "-draft.pdf";

        String s3Url = s3Service.uploadFile(
                new MockMultipartFile(fileName, fileName, "application/pdf", draftPdf),
                folderName,
                true,
                10 * 1024 * 1024
        );

        String s3Key = s3Url.replace(domain + "/", "");

        Certificate certificate = Certificate.builder()
                .certificateId(request.certificateId())
                .studentName(request.studentName())
                .dateOfBirth(request.dateOfBirth())
                .major(request.major())
                .graduationYear(request.graduationYear())
                .gpa(request.gpa())
                .certificateType(request.certificateType())
                .issuer(organization.getCode())
                .issueDate(request.issueDate())
                .pdfFilename(fileName)
                .pdfS3Path(s3Key)
                .pdfS3Url(s3Url)
                .pdfSizeBytes((long) draftPdf.length)
                .draftPdfS3Path(s3Key)
                .status(CertificateStatus.DRAFT)
                .claimCode(null)
                .claimCodeExpiresAt(null)
                .isValid(true)
                .isClaim(false)
                .build();

        Certificate saved = certificateRepository.save(certificate);
        log.info("Draft certificate {} created successfully", saved.getCertificateId());

        return certificateMapper.toResponse(saved);
    }

    @Override
    public List<CertificateResponse> getDraftCertificates() {
        return certificateRepository.findByStatus(CertificateStatus.DRAFT)
                .stream()
                .map(certificateMapper::toResponse)
                .toList();
    }

    @Override
    public List<CertificateResponse> getSignedCertificates() {
        return certificateRepository.findByStatus(CertificateStatus.SIGNED)
                .stream()
                .map(certificateMapper::toResponse)
                .toList();
    }

    @Override
    public List<CertificateResponse> getRevokedCertificates() {
        return certificateRepository.findByStatus(CertificateStatus.REVOKED)
                .stream()
                .map(certificateMapper::toResponse)
                .toList();
    }

    @Override
    public CertificateResponse reissueCertificate(String certificateId) {
        Certificate revokedCertificate = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new CertificateNotFoundException("Certificate not found: " + certificateId));

        if (revokedCertificate.getStatus() != CertificateStatus.REVOKED) {
            throw new IllegalArgumentException("Only REVOKED certificates can be reissued");
        }

        String baseCertificateId = stripReissueSuffix(revokedCertificate.getCertificateId());
        long existingReissues = certificateRepository.countByCertificateIdStartingWith(baseCertificateId + "-R");
        String reissueCertificateId = baseCertificateId + "-R" + (existingReissues + 1);

        String organizationName = organizationRepository.findByCode(revokedCertificate.getIssuer())
                .map(Organization::getName)
                .orElse(revokedCertificate.getIssuer());

        CreateDraftRequest reissueRequest = new CreateDraftRequest(
                reissueCertificateId,
                revokedCertificate.getStudentName(),
                revokedCertificate.getDateOfBirth(),
                revokedCertificate.getMajor(),
                revokedCertificate.getGraduationYear(),
                revokedCertificate.getGpa(),
                revokedCertificate.getCertificateType(),
                revokedCertificate.getIssueDate()
        );

        byte[] draftPdf = pdfService.generateCertificatePdf(toLegacyRequest(reissueRequest), organizationName);
        String folderName = "drafts/" + reissueRequest.issueDate().getYear();
        String fileName = reissueCertificateId + "-draft.pdf";

        String s3Url = s3Service.uploadFile(
                new MockMultipartFile(fileName, fileName, "application/pdf", draftPdf),
                folderName,
                true,
                10 * 1024 * 1024
        );

        String s3Key = s3Url.replace(domain + "/", "");

        Certificate clone = Certificate.builder()
                .certificateId(reissueCertificateId)
                .studentName(revokedCertificate.getStudentName())
                .studentId(revokedCertificate.getStudentId())
                .dateOfBirth(revokedCertificate.getDateOfBirth())
                .major(revokedCertificate.getMajor())
                .graduationYear(revokedCertificate.getGraduationYear())
                .gpa(revokedCertificate.getGpa())
                .certificateType(revokedCertificate.getCertificateType())
                .issuer(revokedCertificate.getIssuer())
                .issueDate(revokedCertificate.getIssueDate())
                .pdfFilename(fileName)
                .pdfS3Path(s3Key)
                .pdfS3Url(s3Url)
                .pdfSizeBytes((long) draftPdf.length)
                .draftPdfS3Path(s3Key)
                .status(CertificateStatus.DRAFT)
                .isValid(true)
                .isClaim(false)
                .build();

        Certificate saved = certificateRepository.save(clone);
        log.info("Reissued certificate {} from {}", saved.getCertificateId(), revokedCertificate.getCertificateId());
        return certificateMapper.toResponse(saved);
    }

    private String stripReissueSuffix(String certificateId) {
        int markerIndex = certificateId.lastIndexOf("-R");
        if (markerIndex <= 0) {
            return certificateId;
        }
        String suffix = certificateId.substring(markerIndex + 2);
        for (int i = 0; i < suffix.length(); i++) {
            if (!Character.isDigit(suffix.charAt(i))) {
                return certificateId;
            }
        }
        return certificateId.substring(0, markerIndex);
    }

    private CertificateRequest toLegacyRequest(CreateDraftRequest request) {
        return CertificateRequest.builder()
                .certificateId(request.certificateId())
                .studentName(request.studentName())
                .dateOfBirth(request.dateOfBirth())
                .major(request.major())
                .graduationYear(request.graduationYear())
                .gpa(request.gpa())
                .certificateType(request.certificateType())
                .issueDate(request.issueDate())
                .build();
    }
}
