package iuh.igc.dto.response.template;

public record TemplateBatchRowErrorResponse(
        Integer rowNumber,
        String certificateId,
        String error
) {
}
