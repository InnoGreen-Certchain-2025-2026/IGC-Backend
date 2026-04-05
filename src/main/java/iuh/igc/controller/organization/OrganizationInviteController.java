package iuh.igc.controller.organization;

import iuh.igc.dto.base.ApiResponse;
import iuh.igc.dto.base.PageResponse;
import iuh.igc.dto.request.organization.CreateOrganizationInviteRequest;
import iuh.igc.dto.response.orginazation.OrganizationInviteResponse;
import iuh.igc.service.organization.OrganizationInviteService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 2/20/2026
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/organizations")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrganizationInviteController {

    OrganizationInviteService organizationInviteService;

    @GetMapping("/invites/users/{userId}")
    public ApiResponse<PageResponse<OrganizationInviteResponse>> getInvitesByUser(
            @PathVariable("userId") Long userId,
            @PageableDefault Pageable pageable
    ) {
        Page<@NonNull OrganizationInviteResponse> page = organizationInviteService
                .getInvitesByUserId(userId, pageable);
        PageResponse<OrganizationInviteResponse> res = new PageResponse<>(page);
        return new ApiResponse<>(res);
    }

    @GetMapping("/{id}/invites")
    public ApiResponse<PageResponse<OrganizationInviteResponse>> getInvitesByOrganization(
            @PathVariable("id") Long id,
            @PageableDefault Pageable pageable
    ) {
        Page<@NonNull OrganizationInviteResponse> page = organizationInviteService
                .getInvitesByOrganization(id, pageable);
        PageResponse<OrganizationInviteResponse> res = new PageResponse<>(page);
        return new ApiResponse<>(res);
    }

    @PostMapping("/{id}/invites")
    public ApiResponse<String> inviteUserToOrganization(
            @PathVariable("id") Long id,
            @RequestBody @Valid CreateOrganizationInviteRequest request
    ) {
        return new ApiResponse<>(organizationInviteService.inviteUser(id, request));
    }

    @PostMapping("/invites/{token}/accept")
    public ApiResponse<@NonNull Void> acceptOrganizationInvite(
            @PathVariable("token") String token
    ) {
        organizationInviteService.acceptInvite(token);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/invites/{token}/decline")
    public ApiResponse<@NonNull Void> declineOrganizationInvite(
            @PathVariable("token") String token
    ) {
        organizationInviteService.declineInvite(token);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/invites/{token}/cancel")
    public ApiResponse<@NonNull Void> cancelOrganizationInvite(
            @PathVariable("token") String token
    ) {
        organizationInviteService.cancelInvite(token);
        return ApiResponse.<Void>builder().build();
    }
}
