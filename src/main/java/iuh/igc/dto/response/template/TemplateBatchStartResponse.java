package iuh.igc.dto.response.template;

public record TemplateBatchStartResponse(
        String batchId,
        String status,
        String message
) {
}
