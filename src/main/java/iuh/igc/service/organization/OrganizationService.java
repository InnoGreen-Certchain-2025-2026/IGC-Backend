package iuh.igc.service.organization;

import iuh.igc.dto.request.organization.CreateOrganizationRequest;
import iuh.igc.dto.request.organization.UpdateOrganizationContactRequest;
import iuh.igc.dto.request.organization.UpdateOrganizationGeneralRequest;
import iuh.igc.dto.request.organization.UpdateOrganizationLegalRequest;
import iuh.igc.dto.response.orginazation.OrganizationResponse;
import iuh.igc.dto.response.orginazation.OrganizationSummaryResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Admin 2/15/2026
 *
 **/
public interface OrganizationService {
    @Transactional
    void createOrganization(
            CreateOrganizationRequest request,
            MultipartFile logoFile
    );

    @Transactional(readOnly = true)
    Page<@NonNull OrganizationSummaryResponse> getUserOrganizations(Pageable pageable);

    @Transactional(readOnly = true)
    List<OrganizationSummaryResponse> getUserBriefOrganizationList();

    @Transactional(readOnly = true)
    OrganizationResponse getUserOrganizationById(Long id);

    @Transactional
    void updateOrganizationGeneral(Long id, UpdateOrganizationGeneralRequest request);

    @Transactional
    void updateOrganizationLegal(Long id, UpdateOrganizationLegalRequest request);

    @Transactional
    void updateOrganizationContact(Long id, UpdateOrganizationContactRequest request);
}
