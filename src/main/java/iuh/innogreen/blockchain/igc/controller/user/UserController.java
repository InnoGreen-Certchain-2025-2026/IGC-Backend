package iuh.innogreen.blockchain.igc.controller.user;

import iuh.innogreen.blockchain.igc.dto.base.ApiResponse;
import iuh.innogreen.blockchain.igc.dto.response.user.UserSessionResponse;
import iuh.innogreen.blockchain.igc.service.user.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
