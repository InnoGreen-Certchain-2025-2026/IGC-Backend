package iuh.igc.service.pdf.impl;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import iuh.igc.dto.request.core.CertificateRequest;
import iuh.igc.service.pdf.PdfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PdfServiceImpl implements PdfService {
    @Value("${blockchain.issuer-name}")
    private String issuerName;
    /**
     * Generate PDF certificate
     */
    @Override
    public byte[] generateCertificatePdf(CertificateRequest request) {
        try {
            log.info("📄 Generating PDF for: {}", request.certificateId());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Set margins
            document.setMargins(50, 50, 50, 50);

            // Colors
            DeviceRgb primaryColor = new DeviceRgb(26, 35, 126); // #1a237e
            DeviceRgb accentColor = new DeviceRgb(63, 81, 181);  // #3f51b5

            // Fonts
            PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold", PdfEncodings.WINANSI);
            PdfFont regularFont = PdfFontFactory.createFont("Helvetica", PdfEncodings.WINANSI);

            // Header - University Name
            Paragraph header = new Paragraph(issuerName.toUpperCase())
                    .setFont(boldFont)
                    .setFontSize(24)
                    .setFontColor(primaryColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            document.add(header);

            // Certificate Title
            Paragraph title = new Paragraph("CERTIFICATE OF COMPLETION")
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setFontColor(accentColor)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            document.add(title);

            // Certificate ID
            Paragraph certId = new Paragraph("Certificate No: " + request.certificateId())
                    .setFont(regularFont)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(certId);

            // Decorative line
            document.add(new Paragraph("\n"));

            // Student Information Table
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);

            addInfoRow(infoTable, "Student Name:", request.studentName(), boldFont, regularFont);
            addInfoRow(infoTable, "Student ID:", request.studentId(), boldFont, regularFont);

            if (request.dateOfBirth() != null) {
                addInfoRow(infoTable, "Date of Birth:",
                        request.dateOfBirth().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        boldFont, regularFont);
            }

            if (request.major() != null) {
                addInfoRow(infoTable, "Major:", request.major(), boldFont, regularFont);
            }

            if (request.graduationYear() != null) {
                addInfoRow(infoTable, "Graduation Year:", request.graduationYear().toString(),
                        boldFont, regularFont);
            }

            if (request.gpa() != null) {
                addInfoRow(infoTable, "GPA:", String.format("%.2f / 4.0", request.gpa()),
                        boldFont, regularFont);
            }

            addInfoRow(infoTable, "Certificate Type:", request.certificateType(),
                    boldFont, regularFont);
            addInfoRow(infoTable, "Issue Date:",
                    request.issueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    boldFont, regularFont);

            document.add(infoTable);

            // Achievement Statement
            Paragraph statement = new Paragraph(
                    "This certifies that the above-named student has successfully completed " +
                            "all requirements and is hereby awarded this " + request.certificateId() + "."
            )
                    .setFont(regularFont)
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.JUSTIFIED)
                    .setMarginTop(20)
                    .setMarginBottom(40);
            document.add(statement);

            // Signature Section
            Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth()
                    .setMarginTop(60);

            // Left signature (Dean)
            Cell deanCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .add(new Paragraph("_______________________")
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(5))
                    .add(new Paragraph("Dean")
                            .setFont(boldFont)
                            .setFontSize(10)
                            .setTextAlignment(TextAlignment.CENTER))
                    .add(new Paragraph(issuerName)
                            .setFont(regularFont)
                            .setFontSize(9)
                            .setTextAlignment(TextAlignment.CENTER));
            signatureTable.addCell(deanCell);

            // Right signature (Rector)
            Cell rectorCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .add(new Paragraph("_______________________")
                            .setTextAlignment(TextAlignment.CENTER)
                            .setMarginBottom(5))
                    .add(new Paragraph("Rector")
                            .setFont(boldFont)
                            .setFontSize(10)
                            .setTextAlignment(TextAlignment.CENTER))
                    .add(new Paragraph(issuerName)
                            .setFont(regularFont)
                            .setFontSize(9)
                            .setTextAlignment(TextAlignment.CENTER));
            signatureTable.addCell(rectorCell);

            document.add(signatureTable);

            // Footer - Blockchain Verification Notice
            Paragraph footer = new Paragraph(
                    "This certificate is cryptographically secured on blockchain. " +
                            "Verify authenticity at: [System URL]"
            )
                    .setFont(regularFont)
                    .setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(40);
            document.add(footer);

            document.close();

            byte[] pdfBytes = baos.toByteArray();
            log.info("✅ PDF generated - Size: {} bytes", pdfBytes.length);

            return pdfBytes;

        } catch (Exception e) {
            log.error("❌ Failed to generate PDF", e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

}

