package iuh.igc.dto.response.orginazation;

import iuh.igc.entity.constant.OrganizationInviteStatus;
import iuh.igc.entity.constant.OrganizationRole;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Admin 2/22/2026
 */
@Builder
public record OrganizationInviteResponse(
        Long id,
        String inviteToken,
        Long organizationId,
        String organizationName,
        String organizationCode,
        String organizationLogoUrl,
        String inviteeEmail,
        String inviterName,
        String inviterEmail,
        OrganizationRole invitedRole,
        OrganizationInviteStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}
