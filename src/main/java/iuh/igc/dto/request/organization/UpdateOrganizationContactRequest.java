package iuh.igc.dto.request.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationContactRequest(
        @NotBlank(message = "Tên liên hệ không được để trống")
        @Size(min = 2, max = 150, message = "Tên liên hệ phải từ 2-150 ký tự")
        String contactName,

        @NotBlank(message = "Email liên hệ không được để trống")
        @Email(message = "Email liên hệ không hợp lệ")
        @Size(max = 254, message = "Email liên hệ tối đa 254 ký tự")
        String contactEmail,

        @NotBlank(message = "SĐT liên hệ không được để trống")
        @Pattern(
                regexp = "^\\+?[1-9]\\d{7,14}$",
                message = "SĐT liên hệ không hợp lệ (khuyến nghị chuẩn E.164, vd: +84901234567)"
        )
        String contactPhone
) {
}
