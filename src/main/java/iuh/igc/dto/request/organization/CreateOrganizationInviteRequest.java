package iuh.igc.dto.request.organization;

import iuh.igc.entity.constant.OrganizationRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrganizationInviteRequest(
        @NotBlank(message = "Email được mời không được để trống")
        @Email(message = "Email được mời không hợp lệ")
        @Size(max = 254, message = "Email được mời tối đa 254 ký tự")
        String inviteeEmail,

        @NotNull(message = "Vai trò được mời không được để trống")
        OrganizationRole invitedRole,

        @Size(max = 500, message = "Lời nhắn mời tối đa 500 ký tự")
        String inviteMessage
) {
}
