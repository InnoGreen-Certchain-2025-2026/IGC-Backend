package iuh.igc.dto.request.user;

import iuh.igc.entity.constant.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import java.time.LocalDate;

/**
 * Admin 2/14/2026
 *
 **/
@Builder
public record UpdateProfileRequest(
        @NotBlank(message = "Tên người dùng không được để trống")
        String name,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^[0-9]{10,15}$", message = "Số điện thoại phải từ 10-15 số")
        String phoneNumber,

        @NotBlank(message = "Địa chỉ người dùng không được để trống")
        String address,

        @NotNull(message = "Ngày sinh không được để trống")
        @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
        LocalDate dob,

        @NotNull(message = "Giới tính không được để trống")
        Gender gender
) {
}
