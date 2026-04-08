package iuh.igc.dto.response.template;

import iuh.igc.entity.template.TemplateField;

import java.time.LocalDateTime;
import java.util.List;

public record TemplateResponse(
        String id,
        Long orgId,
        String name,
        String pdfStorageKey,
        String pdfUrl,
        List<TemplateField> fields,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
