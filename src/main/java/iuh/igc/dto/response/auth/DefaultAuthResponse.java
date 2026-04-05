package iuh.igc.dto.response.auth;

import iuh.igc.dto.response.user.UserSessionResponse;

/**
 * Admin 2/13/2026
 *
 **/
public record DefaultAuthResponse(
        UserSessionResponse userSessionResponse,
        String accessToken
) {
}
