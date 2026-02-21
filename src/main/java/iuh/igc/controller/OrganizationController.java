package iuh.igc.controller;

import iuh.igc.dto.base.ApiResponse;
import iuh.igc.dto.base.PageResponse;
import iuh.igc.dto.request.organization.CreateOrganizationRequest;
import iuh.igc.dto.response.orginazation.OrganizationResponse;
import iuh.igc.dto.response.orginazation.OrganizationSummaryResponse;
import iuh.igc.service.organization.OrganizationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


/**
 * Admin 2/20/2026
 *
 **/
@RestController
@RequiredArgsConstructor
@RequestMapping("/organizations")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrganizationController {

    OrganizationService organizationService;

    @PostMapping
    public ApiResponse<@NonNull Void> createOrganization(
            @RequestPart("data") @Valid CreateOrganizationRequest createOrganizationRequest,
            @RequestPart("logo") MultipartFile logo
    ) {
        organizationService.createOrganization(createOrganizationRequest, logo);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping
    public ApiResponse<PageResponse<OrganizationSummaryResponse>> getUserOrganizations(
            @PageableDefault Pageable pageable
    ) {
        Page<@NonNull OrganizationSummaryResponse> page = organizationService.getUserOrganizations(pageable);
        PageResponse<OrganizationSummaryResponse> res = new PageResponse<>(page);

        return new ApiResponse<>(res);
    }

    @GetMapping("/brief")
    public ApiResponse<List<OrganizationSummaryResponse>> getUserBriefOrganizationList() {
        List<OrganizationSummaryResponse> res = organizationService.getUserBriefOrganizationList();
        return new ApiResponse<>(res);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrganizationResponse> getUserBriefOrganizationList(
            @PathVariable("id") Long id
    ) {
        return new ApiResponse<>(organizationService.getUserOrganizationById(id));
    }


}
