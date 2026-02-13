package iuh.innogreen.blockchain.igc.service.auth;

import iuh.innogreen.blockchain.igc.dto.request.auth.LoginRequest;
import iuh.innogreen.blockchain.igc.dto.request.auth.RegisterRequest;
import iuh.innogreen.blockchain.igc.service.auth.model.AuthResultWrapper;
import org.springframework.http.ResponseCookie;

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
}
