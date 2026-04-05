package iuh.igc.service.organization;

import iuh.igc.dto.request.organization.CreateOrganizationInviteRequest;
import iuh.igc.dto.response.orginazation.OrganizationInviteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface OrganizationInviteService {

    @Transactional(readOnly = true)
    Page<OrganizationInviteResponse> getInvitesByUserId(Long userId, Pageable pageable);

    @Transactional(readOnly = true)
    Page<OrganizationInviteResponse> getInvitesByOrganization(Long organizationId, Pageable pageable);

    @Transactional
    String inviteUser(Long organizationId, CreateOrganizationInviteRequest request);

    @Transactional
    void acceptInvite(String inviteToken);

    @Transactional
    void declineInvite(String inviteToken);

    @Transactional
    void cancelInvite(String inviteToken);
}
