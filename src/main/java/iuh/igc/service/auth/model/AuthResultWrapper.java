package iuh.igc.service.auth.model;

import iuh.igc.dto.response.user.UserSessionResponse;
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
