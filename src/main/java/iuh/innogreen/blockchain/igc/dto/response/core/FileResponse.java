package iuh.innogreen.blockchain.igc.dto.response.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FileResponse(

        @JsonProperty("studentName")
        String studentName,

        @JsonProperty("dateOfBirth")
        String dateOfBirth,       // "2004-07-20"

        @JsonProperty("studentId")
        String studentId,

        @JsonProperty("certificateType")
        String certificateType,

        @JsonProperty("major")
        String major,

        @JsonProperty("issueDate")
        String issueDate,         // "2025-06-20"

        @JsonProperty("certificateId")
        String certificateId,

        @JsonProperty("issuer")
        String issuer
) {}
