package iuh.igc.dto.request.organization;

import iuh.igc.entity.constant.ServicePlan;
import jakarta.validation.constraints.*;

/**
 * Admin 2/15/2026
 *
 **/
public record CreateOrganizationRequest(
        // =========================
        // THÔNG TIN CHUNG
        // =========================
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

        // Optional
        @Size(max = 253, message = "Domain tối đa 253 ký tự")
        @Pattern(
                regexp = "^$|^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$",
                message = "Domain không hợp lệ (vd: example.com)"
        )
        String domain,

        // Optional
        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        String description,

        // =========================
        // THÔNG TIN PHÁP LÝ
        // =========================
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

        // optional
        @Size(max = 150, message = "Tên người đại diện tối đa 150 ký tự")
        String representativeName,

        // =========================
        // THÔNG TIN LIÊN HỆ
        // =========================
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
        String contactPhone,

        // =========================
        // Gói dịch vụ
        // =========================
        @NotNull(message = "Gói dịch vụ không được để trống")
        ServicePlan servicePlan
) {
}
