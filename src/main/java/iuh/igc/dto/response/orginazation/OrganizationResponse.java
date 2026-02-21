package iuh.igc.dto.response.orginazation;

import iuh.igc.entity.constant.ServicePlan;
import lombok.Builder;

/**
 * Admin 2/20/2026
 *
 **/
@Builder
public record OrganizationResponse(
        Long id,

        String name,
        String code,
        String domain,
        String logoUrl,
        String description,

        String legalName,
        String taxCode,
        String legalAddress,
        String representativeName,

        String contactName,
        String contactEmail,
        String contactPhone,

        ServicePlan servicePlan
) {
}
