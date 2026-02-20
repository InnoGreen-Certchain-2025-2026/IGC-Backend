package iuh.innogreen.blockchain.igc.dto.response.orginazation;

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
        String description
) {
}
