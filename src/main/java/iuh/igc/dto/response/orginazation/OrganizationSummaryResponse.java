package iuh.igc.dto.response.orginazation;

import iuh.igc.entity.constant.OrganizationRole;
import lombok.Builder;

/**
 * Admin 2/20/2026
 *
 **/
@Builder
public record OrganizationSummaryResponse(
        Long id,
        String name,
        String code,
        String domain,
        String logoUrl,
        String description,
        OrganizationRole role
) {
}
