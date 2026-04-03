package iuh.igc.dto.response.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import iuh.igc.entity.constant.CertificateStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

@Builder
public record CertificateResponse(

        @JsonProperty("id")
        Long id,

        @JsonProperty("certificateId")
        String certificateId,

        @JsonProperty("studentName")
        String studentName,

        @JsonProperty("studentId")
        Long studentId,

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

        @JsonProperty("signPdfHash")
        String signedPdfHash,

        @JsonProperty("blockchainTxHash")
        String blockchainTxHash,

        @JsonProperty("blockchainBlockNumber")
        Long blockchainBlockNumber,

        @JsonProperty("blockchainTimestamp")
        Long blockchainTimestamp,

        @JsonProperty("isValid")
        Boolean isValid,

        @JsonProperty("status")
        CertificateStatus status,

        @JsonProperty("claimCode")
        String claimCode,

        @JsonProperty("claimCodeExpiresAt")
        LocalDateTime claimCodeExpiresAt,

        @JsonProperty("draftPdfS3Path")
        String draftPdfS3Path,

        @JsonProperty("signedPdfS3Path")
        String signedPdfS3Path,

        @JsonProperty("downloadUrl")
        String downloadUrl,

        @JsonProperty("createdAt")
        LocalDateTime createdAt

) {}