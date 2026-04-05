package iuh.igc.dto.response.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record FileResponse(

        @JsonProperty("studentName")
        String studentName,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("dateofBirth")
        LocalDate dateofBirth,       // "2004-07-20"

        @JsonProperty("certificateType")
        String certificateType,

        @JsonProperty("graduationYear")
        Integer graduationYear,

        @JsonProperty("major")
        String major,

        @JsonFormat(pattern = "yyyy-MM-dd")
        @JsonProperty("issueDate")
        LocalDate issueDate,         // "2025-06-20"

        @JsonProperty("certificateId")
        String certificateId,

        @JsonProperty("gpa")
        Double gpa

) {
}
