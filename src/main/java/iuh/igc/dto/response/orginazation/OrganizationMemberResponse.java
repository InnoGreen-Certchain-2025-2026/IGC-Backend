package iuh.igc.dto.response.orginazation;

import iuh.igc.entity.constant.OrganizationRole;
import lombok.Builder;

/**
 * Admin 2/22/2026
 */
@Builder
public record OrganizationMemberResponse(
        Long userId,
        String name,
        String email,
        String avatarUrl,
        OrganizationRole role
) {
}
