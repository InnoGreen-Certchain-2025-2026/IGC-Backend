package iuh.innogreen.blockchain.igc.dto.response.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
public record VerifyResponse(
        @JsonProperty("exists")
        boolean exists,

        @JsonProperty("valid")
        boolean valid,

        @JsonProperty("certificateId")
        String certificateId,

        @JsonProperty("studentName")
        String studentName,

        @JsonProperty("issuer")
        String issuer,

        @JsonProperty("issueTimestamp")
        Long issueTimestamp,

        @JsonProperty("documentHash")
        String documentHash,

        @JsonProperty("message")
        String message
) { }
