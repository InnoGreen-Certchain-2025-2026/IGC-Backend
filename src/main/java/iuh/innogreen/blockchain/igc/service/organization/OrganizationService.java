package iuh.innogreen.blockchain.igc.service.organization;

import iuh.innogreen.blockchain.igc.dto.request.organization.CreateOrganizationRequest;
import iuh.innogreen.blockchain.igc.dto.response.orginazation.OrganizationResponse;
import iuh.innogreen.blockchain.igc.dto.response.orginazation.OrganizationSummaryResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin 2/15/2026
 *
 **/
public interface OrganizationService {
    @Transactional
    void createOrganization(CreateOrganizationRequest request);

    @Transactional(readOnly = true)
    Page<@NonNull OrganizationSummaryResponse> getUserOrganizations(Pageable pageable);

    @Transactional(readOnly = true)
    List<OrganizationSummaryResponse> getUserBriefOrganizationList();

    @Transactional(readOnly = true)
    OrganizationResponse getUserOrganizationById(Long id);
}
