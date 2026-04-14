package iuh.igc.dto.request.template;

public record CreateTemplateRequest(
        Long orgId,
        String name,
        String pdfStorageKey
) {
}
