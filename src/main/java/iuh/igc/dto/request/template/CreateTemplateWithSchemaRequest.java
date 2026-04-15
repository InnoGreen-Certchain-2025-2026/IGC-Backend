package iuh.igc.dto.request.template;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateTemplateWithSchemaRequest(
        @NotNull(message = "orgId is required")
        Long orgId,

        @NotBlank(message = "name is required")
        String name,

        @NotNull(message = "fields is required")
        @Size(min = 1, message = "fields must have at least 1 item")
        List<@Valid TemplateFieldRequest> fields
) {
}
