package iuh.innogreen.blockchain.igc.service.auth.model;

import iuh.innogreen.blockchain.igc.dto.response.user.UserSessionResponse;
import lombok.Builder;
import org.springframework.http.ResponseCookie;

/**
 * Admin 2/13/2026
 *
 **/
@Builder
public record AuthResultWrapper(
        UserSessionResponse userSessionResponse,
        String accessToken,
        ResponseCookie refreshTokenCookie
) {
}
