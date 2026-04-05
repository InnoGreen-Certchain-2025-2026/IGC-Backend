package iuh.igc.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Admin 2/13/2026
 *
 **/
@Builder
public record LoginRequest(
        @NotBlank(message = "Email người dùng không được để trống")
        @Email(message = "Định dạng email không hợp lệ")
        String email,

        @NotBlank(message = "Mật khẩu người dùng không được để trống")
        String password
) {
}
