package iuh.igc.controller;

import iuh.igc.dto.base.ApiResponse;
import iuh.igc.dto.request.user.UpdateProfileRequest;
import iuh.igc.dto.response.user.UserProfileResponse;
import iuh.igc.dto.response.user.UserSessionResponse;
import iuh.igc.service.user.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin 2/13/2026
 *
 **/
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @GetMapping("/me")
    public ApiResponse<@NonNull UserSessionResponse> getCurrentUser() {
        return new ApiResponse<>(userService.getUserSession());
    }

    @GetMapping("/me/profile")
    public ApiResponse<@NonNull UserProfileResponse> getUserProfile() {
        return new ApiResponse<>(userService.getUserProfile());
    }

    @PostMapping("/me/profile")
    public ApiResponse<@NonNull Void> updateUserProfile(
            @RequestBody
            UpdateProfileRequest updateProfileRequest
    ) {
        userService.updateUserProfile(updateProfileRequest);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/me/avatar")
    public ApiResponse<@NonNull Void> updateUserAvatar(
            @RequestPart("avatar") MultipartFile avatar
    ) {
        userService.updateUserAvatar(avatar);
        return ApiResponse.<Void>builder().build();
    }
}
