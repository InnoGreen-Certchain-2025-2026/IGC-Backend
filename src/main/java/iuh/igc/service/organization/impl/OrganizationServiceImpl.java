package iuh.igc.service.organization.impl;

import iuh.igc.config.s3.S3Service;
import iuh.igc.dto.request.organization.CreateOrganizationRequest;
import iuh.igc.dto.response.orginazation.OrganizationResponse;
import iuh.igc.dto.response.orginazation.OrganizationSummaryResponse;
import iuh.igc.entity.organization.Organization;
import iuh.igc.entity.organization.OrganizationMember;
import iuh.igc.entity.User;
import iuh.igc.entity.constant.OrganizationRole;
import iuh.igc.repository.OrganizationMemberRepository;
import iuh.igc.repository.OrganizationRepository;
import iuh.igc.service.organization.OrganizationService;
import iuh.igc.service.user.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Admin 2/15/2026
 *
 **/
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrganizationServiceImpl implements OrganizationService {

    // Repository
    OrganizationMemberRepository organizationMemberRepository;
    OrganizationRepository organizationRepository;

    // Provider
    CurrentUserProvider currentUserProvider;

    // Constant
    static Long MAX_LOGO_FILE_SIZE = 5 * 1024 * 1024L;
    private final S3Service s3Service;

    /**
     * =============================================
     * Tạo tổ chức
     * =============================================
     **/

    @Transactional
    @Override
    public void createOrganization(
            CreateOrganizationRequest request,
            MultipartFile logoFile
    ) {

        // Check ràng buộc
        String code = request.code();
        String domain = request.domain() != null && !request.domain().isBlank()
                ? request.domain().trim().toLowerCase()
                : null;
        String taxCode = request.taxCode();

        if (organizationRepository.existsByCode(code))
            throw new DataIntegrityViolationException("Mã tổ chức đã tồn tại");

        if (domain != null && organizationRepository.existsByDomain(domain))
            throw new DataIntegrityViolationException("Domain đã tồn tại");

        if (organizationRepository.existsByTaxCode(taxCode))
            throw new DataIntegrityViolationException("Mã số thuế đã tồn tại");

        Organization organization = new Organization();

        // Set thông tin chung
        organization.setName(request.name());
        organization.setCode(request.code());
        organization.setDomain(domain);

        String description = request.description() != null && !request.description().isBlank()
                ? request.description()
                : null;
        organization.setDescription(description);

        // Set thông tin pháp lý
        organization.setLegalName(request.legalName());
        organization.setTaxCode(request.taxCode());
        organization.setLegalAddress(request.legalAddress());
        organization.setRepresentativeName(request.representativeName());

        // Set thông tin liên hệ
        organization.setContactName(request.contactName());
        organization.setContactEmail(request.contactEmail());
        organization.setContactPhone(request.contactPhone());

        // Set gói dịch vụ
        organization.setServicePlan(request.servicePlan());

        Organization savedOrganization = organizationRepository.save(organization);

        // Set logo
        if (logoFile != null && !logoFile.isEmpty()) {
            String folderName = "organizations/" + savedOrganization.getId() + "/logo";
            String logoUrl = s3Service.uploadFile(
                    logoFile,
                    folderName,
                    false,
                    MAX_LOGO_FILE_SIZE
            );
            savedOrganization.setLogoUrl(logoUrl);
        }


        // Gán quyền OWNER cho người dùng
        User user = currentUserProvider.get();

        OrganizationMember owner = new OrganizationMember();
        owner.setOrganization(savedOrganization);
        owner.setUser(user);
        owner.setOrganizationRole(OrganizationRole.OWNER);
        organizationMemberRepository.save(owner);
    }

    /**
     * =============================================
     * Lấy thông tin tổ chức
     * Note: chỉ lấy tổ chức mà thuộc về người dùng đó
     * =============================================
     **/

    @Transactional(readOnly = true)
    @Override
    public Page<@NonNull OrganizationSummaryResponse> getUserOrganizations(Pageable pageable) {
        User user = currentUserProvider.get();

        return organizationRepository
                .findDistinctByOrganizationMembers_User_Id(user.getId(), pageable)
                .map(this::mapToOrganizationSummaryResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public List<OrganizationSummaryResponse> getUserBriefOrganizationList() {
        User user = currentUserProvider.get();
        Pageable pageable = PageRequest.of(0, 8);

        return organizationRepository
                .findDistinctByOrganizationMembers_User_IdOrderByNameDesc(user.getId(), pageable)
                .map(this::mapToOrganizationSummaryResponse)
                .stream()
                .toList();

    }

    @Transactional(readOnly = true)
    @Override
    public OrganizationResponse getUserOrganizationById(Long id) {
        User user = currentUserProvider.get();

        Organization organization = organizationRepository
                .findByIdAndOrganizationMembers_User_Id(id, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tổ chức"));

        return mapToOrganizationResponse(organization);
    }

    /**
     * =============================================
     * Mapper
     * =============================================
     **/
    private OrganizationSummaryResponse mapToOrganizationSummaryResponse(Organization organization) {
        return OrganizationSummaryResponse
                .builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .code(organization.getCode())
                .domain(organization.getDomain())
                .logoUrl(organization.getLogoUrl())
                .description(organization.getDescription())
                .build();
    }

    private OrganizationResponse mapToOrganizationResponse(Organization organization) {
        return OrganizationResponse
                .builder()
                .id(organization.getId())
                .name(organization.getName())
                .code(organization.getCode())
                .domain(organization.getDomain())
                .logoUrl(organization.getLogoUrl())
                .description(organization.getDescription())
                .legalName(organization.getLegalName())
                .taxCode(organization.getTaxCode())
                .legalAddress(organization.getLegalAddress())
                .representativeName(organization.getRepresentativeName())
                .contactName(organization.getContactName())
                .contactEmail(organization.getContactEmail())
                .contactPhone(organization.getContactPhone())
                .servicePlan(organization.getServicePlan())
                .build();
    }


}
