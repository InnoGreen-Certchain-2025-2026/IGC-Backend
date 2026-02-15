package iuh.innogreen.blockchain.igc.service.organization.impl;

import iuh.innogreen.blockchain.igc.dto.request.organization.CreateOrganizationRequest;
import iuh.innogreen.blockchain.igc.entity.Organization;
import iuh.innogreen.blockchain.igc.entity.OrganizationMember;
import iuh.innogreen.blockchain.igc.entity.User;
import iuh.innogreen.blockchain.igc.entity.constant.OrganizationRole;
import iuh.innogreen.blockchain.igc.repository.OrganizationMemberRepository;
import iuh.innogreen.blockchain.igc.repository.OrganizationRepository;
import iuh.innogreen.blockchain.igc.service.user.CurrentUserProvider;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin 2/15/2026
 *
 **/
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrganizationServiceImpl {

    // Repository
    OrganizationMemberRepository organizationMemberRepository;
    OrganizationRepository organizationRepository;

    // Provider
    CurrentUserProvider currentUserProvider;

    @Transactional
    public void createOrganization(CreateOrganizationRequest request) {

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

        // Set thông tin phát hành/ ký
        organization.setBlockchainContractAddress("1");
        organization.setBlockchainAdminAddress("1");
        organization.setChainId("1");

        // Set gói dịch vụ
        organization.setServicePlan(request.servicePlan());

        Organization savedOrganization = organizationRepository.save(organization);

        // Gán quyền OWNER cho người dùng
        User user = currentUserProvider.get();

        OrganizationMember owner = new OrganizationMember();
        owner.setOrganization(savedOrganization);
        owner.setUser(user);
        owner.setOrganizationRole(OrganizationRole.OWNER);
        organizationMemberRepository.save(owner);
    }

}
