package iuh.igc.service.auth;

import iuh.igc.dto.request.auth.LoginRequest;
import iuh.igc.dto.request.auth.RegisterRequest;
import iuh.igc.dto.request.user.UpdatePasswordRequest;
import iuh.igc.service.auth.model.AuthResultWrapper;
import org.springframework.http.ResponseCookie;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin 2/13/2026
 *
 **/
public interface AuthService {
    void register(RegisterRequest request);

    AuthResultWrapper login(LoginRequest loginRequest);

    ResponseCookie logout(String refreshToken);

    AuthResultWrapper refreshSession(
            String refreshToken
    );

    @Transactional
    void updatePassword(UpdatePasswordRequest updatePasswordRequest);
}
