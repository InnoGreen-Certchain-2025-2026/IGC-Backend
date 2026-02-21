package iuh.innogreen.blockchain.igc.dto.response.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record CertificateResponse(

        @JsonProperty("id")
        Long id,

        @JsonProperty("certificateId")
        String certificateId,

        @JsonProperty("studentName")
        String studentName,

        @JsonProperty("studentId")
        String studentId,

        @JsonProperty("dateOfBirth")
        LocalDate dateOfBirth,

        @JsonProperty("major")
        String major,

        @JsonProperty("graduationYear")
        Integer graduationYear,

        @JsonProperty("gpa")
        Double gpa,

        @JsonProperty("certificateType")
        String certificateType,

        @JsonProperty("issuer")
        String issuer,

        @JsonProperty("issueDate")
        LocalDate issueDate,

        @JsonProperty("documentHash")
        String documentHash,

        @JsonProperty("blockchainTxHash")
        String blockchainTxHash,

        @JsonProperty("blockchainBlockNumber")
        Long blockchainBlockNumber,

        @JsonProperty("blockchainTimestamp")
        Long blockchainTimestamp,

        @JsonProperty("isValid")
        Boolean isValid,

        @JsonProperty("createdAt")
        LocalDateTime createdAt

) {}