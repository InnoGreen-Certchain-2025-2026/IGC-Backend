package iuh.innogreen.blockchain.igc.dto.request.core;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
public record CertificateRequest(
        @NotBlank(message = "Certificate ID is required")
        String certificateId,

        @NotBlank(message = "Student name is required")
        String studentName,

        @NotBlank(message = "Student ID is required")
        String studentId,

        LocalDate dateOfBirth,

        String major,

        Integer graduationYear,

        @DecimalMin(value = "0.0")
        @DecimalMax(value = "4.0")
        Double gpa,

        //Update later: Enum
        @NotBlank(message = "Certificate type is required")
        String certificateType,

        @NotNull(message = "Issue date is required")
        LocalDate issueDate)
{
}
