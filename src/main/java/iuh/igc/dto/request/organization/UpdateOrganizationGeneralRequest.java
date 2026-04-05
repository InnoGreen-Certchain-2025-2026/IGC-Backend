package iuh.igc.dto.request.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationGeneralRequest(
        @NotBlank(message = "Tên tổ chức không được để trống")
        @Size(min = 2, max = 200, message = "Tên tổ chức phải từ 2-200 ký tự")
        String name,

        @NotBlank(message = "Mã tổ chức không được để trống")
        @Size(min = 3, max = 32, message = "Mã tổ chức phải từ 3-32 ký tự")
        @Pattern(
                regexp = "^[A-Z0-9_-]{3,32}$",
                message = "Mã tổ chức chỉ gồm A-Z, 0-9, _ và - (không dấu cách)"
        )
        String code,

        @Size(max = 253, message = "Domain tối đa 253 ký tự")
        @Pattern(
                regexp = "^$|^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$",
                message = "Domain không hợp lệ (vd: example.com)"
        )
        String domain,

        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        String description
) {
}
