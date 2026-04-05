package iuh.igc.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Admin 2/14/2026
 *
 **/
@Builder
public record UpdatePasswordRequest(

        @NotBlank(message = "Mật khẩu cũ không được để trống")
        String oldPassword,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        String newPassword,

        @NotBlank(message = "Nhập lại mật khẩu mới được để trống")
        String confirmedNewPassword

) {
}
