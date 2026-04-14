package iuh.igc.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SyncRequest {

    @NotBlank(message = "Cognito Subject is required")
    String cognitoSub;

    @NotBlank(message = "Email is required")
    String email;

    @NotBlank(message = "Name is required")
    String name;

}
