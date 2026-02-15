package iuh.innogreen.blockchain.igc.dto.base;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record FileResponse(
        @JsonProperty("studentName")
        String studentName,

        @JsonProperty("dateOfBirth")
        String dateOfBirth,

        @JsonProperty("studentId")
        String studentId,

        @JsonProperty("certificateType")
        String certificateType,

        @JsonProperty("major")
        String major,

        @JsonProperty("issueDate")
        String issueDate,

        @JsonProperty("certificateId")
        String certificateId,

        @JsonProperty("issuer")
        String issuer
) {
}
