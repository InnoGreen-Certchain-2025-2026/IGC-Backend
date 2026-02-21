package iuh.igc.dto.response.user;

import lombok.Builder;

/**
 * Admin 2/13/2026
 *
 **/
@Builder
public record UserSessionResponse(
        String email,
        String name,
        String avatarUrl
) {
}
