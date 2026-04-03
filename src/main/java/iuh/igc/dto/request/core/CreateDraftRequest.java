package iuh.igc.dto.request.core;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateDraftRequest(
        @NotBlank(message = "Certificate ID is required")
        String certificateId,

        @NotBlank(message = "Student name is required")
        String studentName,

        LocalDate dateOfBirth,

        String major,

        Integer graduationYear,

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "4.0")
        Double gpa,

        @NotBlank(message = "Certificate type is required")
        String certificateType,

        @NotNull(message = "Issue date is required")
        LocalDate issueDate
) {
}
