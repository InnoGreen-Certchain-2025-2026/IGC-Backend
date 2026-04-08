package iuh.igc.service.pdf;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import iuh.igc.dto.request.core.CertificateRequest;
import iuh.igc.dto.request.core.SignaturePosition;
import iuh.igc.entity.template.TemplateField;

import java.util.List;
import java.util.Map;

public interface PdfService {
    byte[] generateCertificatePdf(CertificateRequest request, String vendorName);

        byte[] addSignatureImageToPdf(byte[] pdfBytes, byte[] imageBytes, SignaturePosition position);

    byte[] renderTemplatePdf(
            byte[] templatePdfBytes,
            List<TemplateField> fields,
            Map<String, String> values,
            byte[] signatureImageBytes
    );

    default void addInfoRow(Table table, String label, String value, PdfFont boldFont, PdfFont regularFont) {
        table.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(label)
                        .setFont(boldFont)
                        .setFontSize(11)));

        table.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(value)
                        .setFont(regularFont)
                        .setFontSize(11)));
    }
}
