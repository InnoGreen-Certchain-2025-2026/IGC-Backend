package iuh.igc.service.pdf;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import iuh.igc.dto.request.core.CertificateRequest;

public interface PdfService {
    byte[] generateCertificatePdf(CertificateRequest request, String vendorName);

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
