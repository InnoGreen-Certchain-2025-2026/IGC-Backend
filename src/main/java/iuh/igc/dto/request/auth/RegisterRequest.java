package iuh.igc.dto.request.auth;

import iuh.igc.entity.constant.Gender;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

/**
 * Admin 2/13/2026
 *
 **/
public record RegisterRequest(
        @NotBlank(message = "Email người dùng không được để trống")
        @Email(message = "Định dạng email không hợp lệ")
        String email,

        @NotBlank(message = "Tên người dùng không được để trống")
        String name,

        @NotBlank(message = "Mã căn cước công dân không được để trống")
        @Pattern(regexp = "^[0-9]{12}$", message = "Mã căn cước công dân phải có đúng 12 số")
        String citizenIdNumber,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^[0-9]{10,15}$", message = "Số điện thoại phải từ 10-15 số")
        String phoneNumber,

        @NotBlank(message = "Địa chỉ người dùng không được để trống")
        String address,

        @NotNull(message = "Ngày sinh không được để trống")
        @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
        LocalDate dob,

        @NotNull(message = "Giới tính không được để trống")
        Gender gender,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password
) {
}
