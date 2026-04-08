package iuh.igc.dto.request.template;

import jakarta.validation.constraints.*;

public record TemplateFieldRequest(
        String id,

        @NotBlank(message = "field name is required")
        String name,

        @NotBlank(message = "field type is required")
        @Pattern(regexp = "^(text|date|number|image)$",
                message = "type must be one of: text, date, number, image")
        String type,

        @NotNull(message = "x is required")
        @DecimalMin(value = "0.0", message = "x must be >= 0")
        @DecimalMax(value = "100.0", message = "x must be <= 100")
        Double x,

        @NotNull(message = "y is required")
        @DecimalMin(value = "0.0", message = "y must be >= 0")
        @DecimalMax(value = "100.0", message = "y must be <= 100")
        Double y,

        @NotNull(message = "w is required")
        @DecimalMin(value = "0.1", message = "w must be > 0")
        @DecimalMax(value = "100.0", message = "w must be <= 100")
        Double w,

        @NotNull(message = "h is required")
        @DecimalMin(value = "0.1", message = "h must be > 0")
        @DecimalMax(value = "100.0", message = "h must be <= 100")
        Double h,

        Integer fontSize,
        String fontFamily,
        String align,
        String color
) {
}
