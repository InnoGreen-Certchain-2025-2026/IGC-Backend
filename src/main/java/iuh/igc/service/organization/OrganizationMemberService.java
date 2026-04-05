package iuh.igc.service.organization;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import iuh.igc.dto.response.orginazation.OrganizationMemberResponse;

public interface OrganizationMemberService {

    @Transactional(readOnly = true)
    Page<OrganizationMemberResponse> getOrganizationMembers(Long organizationId, Pageable pageable);

    @Transactional
    void promoteToModerator(Long organizationId, Long targetUserId);

    @Transactional
    void demoteToMember(Long organizationId, Long targetUserId);

    @Transactional
    void kickMember(Long organizationId, Long targetUserId);
}
