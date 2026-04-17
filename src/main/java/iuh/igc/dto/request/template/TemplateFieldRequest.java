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

        @Min(value = 6, message = "fontSize must be >= 6")
        @Max(value = 72, message = "fontSize must be <= 72")
        Integer fontSize,

        @Pattern(
                regexp = "(?i)^\\s*$|^(helvetica|arial|sans-serif|sans|system-ui|helvetica-bold|arial-bold|sans-bold|times|times-roman|times new roman|serif|times-bold|times new roman bold|serif-bold|courier|monospace|mono|courier-bold|mono-bold)$",
                message = "fontFamily is invalid"
        )
        String fontFamily,

        @Pattern(
                regexp = "(?i)^\\s*$|^(left|center|right)$",
                message = "align must be one of: left, center, right"
        )
        String align,

        @Pattern(
                regexp = "(?i)^\\s*$|^#?[0-9a-f]{6}$",
                message = "color must be a valid hex color, example: #1A2B3C"
        )
        String color
) {
}
