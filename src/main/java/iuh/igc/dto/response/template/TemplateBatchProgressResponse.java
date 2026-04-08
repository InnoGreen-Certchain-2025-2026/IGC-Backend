package iuh.igc.dto.response.template;

import java.time.LocalDateTime;
import java.util.List;

public record TemplateBatchProgressResponse(
        String batchId,
        String status,
        Integer totalRows,
        Integer processedRows,
        Integer successCount,
        Integer failureCount,
        Integer progressPercent,
        String currentMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<TemplateBatchRowErrorResponse> errors
) {
}
