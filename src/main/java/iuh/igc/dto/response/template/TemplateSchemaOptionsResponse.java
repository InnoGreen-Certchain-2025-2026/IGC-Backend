package iuh.igc.dto.response.template;

import java.util.List;

public record TemplateSchemaOptionsResponse(
        List<String> fontFamilies,
        List<String> alignments,
        int minFontSize,
        int maxFontSize,
        int defaultFontSize
) {
}
