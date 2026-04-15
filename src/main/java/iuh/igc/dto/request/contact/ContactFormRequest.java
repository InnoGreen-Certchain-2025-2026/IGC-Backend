package iuh.igc.dto.request.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactFormRequest(
        @NotBlank(message = "Ho va ten khong duoc de trong") @Size(max = 150, message = "Ho va ten toi da 150 ky tu") String fullName,

        @NotBlank(message = "Email khong duoc de trong") @Email(message = "Email khong hop le") @Size(max = 254, message = "Email toi da 254 ky tu") String email,

        @NotBlank(message = "Ten cong ty khong duoc de trong") @Size(max = 200, message = "Ten cong ty toi da 200 ky tu") String company,

        @NotBlank(message = "Noi dung khong duoc de trong") @Size(max = 2000, message = "Noi dung toi da 2000 ky tu") String description) {
}
