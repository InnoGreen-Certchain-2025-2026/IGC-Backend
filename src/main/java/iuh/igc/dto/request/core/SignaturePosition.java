package iuh.igc.dto.request.core;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record SignaturePosition(
        @NotNull(message = "Signature position x is required")
        @PositiveOrZero(message = "x must be >= 0")
        Float x,

        @NotNull(message = "Signature position y is required")
        @PositiveOrZero(message = "y must be >= 0")
        Float y,

        @NotNull(message = "Signature width is required")
        @Positive(message = "width must be > 0")
        Float width,

        @NotNull(message = "Signature height is required")
        @Positive(message = "height must be > 0")
        Float height
) {
}
