package iuh.innogreen.blockchain.igc.controller.auth;

import iuh.innogreen.blockchain.igc.dto.base.ApiResponse;
import iuh.innogreen.blockchain.igc.dto.request.auth.LoginRequest;
import iuh.innogreen.blockchain.igc.dto.request.auth.RegisterRequest;
import iuh.innogreen.blockchain.igc.dto.response.auth.DefaultAuthResponse;
import iuh.innogreen.blockchain.igc.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 2/13/2026
 *
 **/
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;
    
    @PostMapping("/register")
    public ApiResponse<Void> register(
            @RequestBody @Valid RegisterRequest registerRequest
    ) {
        authService.register(registerRequest);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/login")
    public ResponseEntity<@NonNull ApiResponse<DefaultAuthResponse>> login(
            @RequestBody @Valid LoginRequest loginRequest
    ) {
        var authResultWrapper = authService.login(loginRequest);
        var payload = new DefaultAuthResponse(
                authResultWrapper.userSessionResponse(),
                authResultWrapper.accessToken()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, authResultWrapper.refreshTokenCookie().toString())
                .body(new ApiResponse<>(payload));
    }

    @PostMapping("/logout")
    public ResponseEntity<@NonNull Void> logout(
            @CookieValue(value = "refresh_token", required = false) String refreshToken
    ) {
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .header(HttpHeaders.SET_COOKIE, authService.logout(refreshToken).toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<@NonNull ApiResponse<DefaultAuthResponse>> refreshSession(
            @CookieValue(value = "refresh_token") String refreshToken
    ) {
        var authResultWrapper = authService.refreshSession(refreshToken);
        var payload = new DefaultAuthResponse(
                authResultWrapper.userSessionResponse(),
                authResultWrapper.accessToken()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, authResultWrapper.refreshTokenCookie().toString())
                .body(new ApiResponse<>(payload));
    }

}
