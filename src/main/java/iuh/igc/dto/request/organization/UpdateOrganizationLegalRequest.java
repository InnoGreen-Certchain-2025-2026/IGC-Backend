package iuh.igc.dto.request.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationLegalRequest(
        @NotBlank(message = "Tên pháp lý không được để trống")
        @Size(min = 2, max = 255, message = "Tên pháp lý phải từ 2-255 ký tự")
        String legalName,

        @NotBlank(message = "Mã số thuế không được để trống")
        @Pattern(
                regexp = "^(\\d{10})(-?\\d{3})?$",
                message = "Mã số thuế không hợp lệ (10 số hoặc 10 số + 3 số chi nhánh)"
        )
        String taxCode,

        @NotBlank(message = "Địa chỉ pháp lý không được để trống")
        @Size(min = 5, max = 500, message = "Địa chỉ pháp lý phải từ 5-500 ký tự")
        String legalAddress,

        @Size(max = 150, message = "Tên người đại diện tối đa 150 ký tự")
        String representativeName
) {
}
