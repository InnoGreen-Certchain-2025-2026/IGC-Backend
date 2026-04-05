package iuh.igc.dto.response.user;

import lombok.Builder;

/**
 * Admin 2/13/2026
 *
 **/
@Builder
public record UserSessionResponse(
        Long id,
        String email,
        String name,
        String avatarUrl
) {
}
