package iuh.igc.dto.response.user;

import iuh.igc.entity.constant.Gender;
import lombok.Builder;

import java.time.LocalDate;

/**
 * Admin 2/14/2026
 *
 **/
@Builder
public record UserProfileResponse(
        String name,
        String phoneNumber,
        String address,
        LocalDate dob,
        Gender gender
) {
}
