package iuh.innogreen.blockchain.igc.dto.base;

import java.time.LocalDate;

public record FileResponse(
    String studentName,
    LocalDate dateOfBirth,
    String studentId,
    String certificateType,
    String major,
    LocalDate issueDate,
    String certificateId,
    String issuer
) {
}
