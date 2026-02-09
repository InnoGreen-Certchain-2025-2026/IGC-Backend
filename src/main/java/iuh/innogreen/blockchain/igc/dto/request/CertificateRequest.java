package iuh.innogreen.blockchain.igc.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRequest {

    @NotBlank(message = "Certificate ID is required")
    private String certificateId;

    @NotBlank(message = "Student name is required")
    private String studentName;

    @NotBlank(message = "Student ID is required")
    private String studentId;

    private LocalDate dateOfBirth;

    private String major;

    private Integer graduationYear;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "4.0")
    private Double gpa;

    @NotBlank(message = "Certificate type is required")
    private String certificateType;

    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;
}
