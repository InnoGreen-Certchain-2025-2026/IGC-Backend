package iuh.igc.service.pdf.impl;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.signatures.SignatureUtil;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import iuh.igc.dto.request.core.CertificateRequest;
import iuh.igc.dto.request.core.SignaturePosition;
import iuh.igc.entity.template.TemplateField;
import iuh.igc.service.pdf.PdfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class PdfServiceImpl implements PdfService {
    @Value("${blockchain.issuer-name}")
    private String issuerName;
    /**
     * Generate PDF certificate
     */
    @Override
    public byte[] generateCertificatePdf(CertificateRequest request, String vendorName) {
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
            Paragraph header = new Paragraph(vendorName.toUpperCase())
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
            //addInfoRow(infoTable, "Student ID:", request.studentId(), boldFont, regularFont);

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

        @Override
        public byte[] addSignatureImageToPdf(byte[] pdfBytes, byte[] imageBytes, SignaturePosition position) {
                try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        PdfDocument pdfDocument = new PdfDocument(
                                        new PdfReader(new java.io.ByteArrayInputStream(pdfBytes)),
                                        new PdfWriter(baos)
                        );

                        Document document = new Document(pdfDocument);
                        ImageData imageData = ImageDataFactory.create(imageBytes);
                        Image image = new Image(imageData)
                                        .setFixedPosition(
                                                        1,
                                                        position.x(),
                                                        position.y(),
                                                        position.width()
                                        )
                                        .setHeight(position.height());

                        document.add(image);
                        document.close();

                        return baos.toByteArray();
                } catch (Exception e) {
                        log.error("Failed to add signature image to PDF", e);
                        throw new RuntimeException("Failed to add signature image to PDF", e);
                }
        }

        @Override
        public byte[] renderTemplatePdf(byte[] templatePdfBytes,
                                        List<TemplateField> fields,
                                        Map<String, String> values,
                                        byte[] signatureImageBytes) {
                try {
                        // Clean any existing signatures from template PDF before rendering
                        byte[] cleanPdfBytes = cleanSignaturesFromPdf(templatePdfBytes);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        PdfDocument pdfDocument = new PdfDocument(
                                new PdfReader(new java.io.ByteArrayInputStream(cleanPdfBytes)),
                                new PdfWriter(baos)
                        );

                        Document document = new Document(pdfDocument);

                        PdfPage firstPage = pdfDocument.getFirstPage();
                        var pageSize = firstPage.getPageSize();
                        float pageWidth = pageSize.getWidth();
                        float pageHeight = pageSize.getHeight();

                        PdfFont defaultFont = PdfFontFactory.createFont(StandardFonts.HELVETICA, PdfEncodings.WINANSI);

                        for (TemplateField field : fields) {
                                if (field == null || field.getName() == null || field.getType() == null) {
                                        continue;
                                }

                                float x = (float) (field.getX() / 100.0 * pageWidth);
                                float yTop = (float) (field.getY() / 100.0 * pageHeight);
                                float width = (float) (field.getW() / 100.0 * pageWidth);
                                float height = (float) (field.getH() / 100.0 * pageHeight);
                                float yBottom = pageHeight - yTop - height;

                                String type = field.getType().toLowerCase(Locale.ROOT);
                                if ("image".equals(type)) {
                                        if (signatureImageBytes == null || signatureImageBytes.length == 0) {
                                                continue;
                                        }

                                        Image image = new Image(ImageDataFactory.create(signatureImageBytes))
                                                .setFixedPosition(1, x, yBottom, width)
                                                .setHeight(height);

                                        document.add(image);
                                        continue;
                                }

                                String value = values.getOrDefault(field.getName(),
                                                values.getOrDefault(field.getName().toLowerCase(Locale.ROOT), ""));

                                PdfFont fieldFont = resolveTemplateFieldFont(field.getFontFamily(), defaultFont);
                                float fieldFontSize = normalizeTemplateFieldFontSize(field.getFontSize());

                                Paragraph paragraph = new Paragraph(value)
                                        .setFont(fieldFont)
                                        .setFontSize(fieldFontSize)
                                        .setMargin(0)
                                        .setFontColor(parseColor(field.getColor()));

                                String align = field.getAlign() == null ? "left" : field.getAlign().toLowerCase(Locale.ROOT);
                                if ("center".equals(align)) {
                                        paragraph.setTextAlignment(TextAlignment.CENTER);
                                } else if ("right".equals(align)) {
                                        paragraph.setTextAlignment(TextAlignment.RIGHT);
                                } else {
                                        paragraph.setTextAlignment(TextAlignment.LEFT);
                                }

                                Rectangle fieldBox = new Rectangle(x, yBottom, width, height);
                                Canvas canvas = new Canvas(firstPage, fieldBox);
                                canvas.add(paragraph);
                                canvas.close();
                        }

                        document.close();
                        return baos.toByteArray();
                } catch (Exception e) {
                        log.error("Failed to render template PDF", e);
                        throw new RuntimeException("Failed to render template PDF", e);
                }
        }

        /**
         * Remove all digital signatures from PDF to ensure clean state for new signing
         * This prevents multi-revision issues when re-signing PDFs
         */
        private byte[] cleanSignaturesFromPdf(byte[] pdfBytes) {
                try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        try (PdfDocument doc = new PdfDocument(
                                new PdfReader(new java.io.ByteArrayInputStream(pdfBytes)),
                                new PdfWriter(baos))) {

                                SignatureUtil signUtil = new SignatureUtil(doc);
                                java.util.List<String> signatureNames = signUtil.getSignatureNames();

                                if (!signatureNames.isEmpty()) {
                                        log.info("🧹 Removing {} signatures from template PDF", signatureNames.size());
                                        // Simply rewriting without signature fields produces clean PDF
                                }
                        }

                        return baos.toByteArray();
                } catch (Exception e) {
                        log.warn("Failed to clean signatures from template PDF, using original", e);
                        return pdfBytes;  // Fallback to original if cleaning fails
                }
        }

        private Color parseColor(String hex) {
                if (hex == null || hex.isBlank()) {
                        return ColorConstants.BLACK;
                }
                String value = hex.trim();
                if (value.startsWith("#")) {
                        value = value.substring(1);
                }
                if (value.length() != 6) {
                        return ColorConstants.BLACK;
                }
                try {
                        int r = Integer.parseInt(value.substring(0, 2), 16);
                        int g = Integer.parseInt(value.substring(2, 4), 16);
                        int b = Integer.parseInt(value.substring(4, 6), 16);
                        return new DeviceRgb(r, g, b);
                } catch (Exception ignored) {
                        return ColorConstants.BLACK;
                }
        }

        private PdfFont resolveTemplateFieldFont(String fontFamily, PdfFont fallback) {
                if (fontFamily == null || fontFamily.isBlank()) {
                        return fallback;
                }

                String normalized = fontFamily.trim().toLowerCase(Locale.ROOT);
                String standardFont = switch (normalized) {
                        case "helvetica", "arial", "sans-serif", "sans", "system-ui" -> StandardFonts.HELVETICA;
                        case "helvetica-bold", "arial-bold", "sans-bold" -> StandardFonts.HELVETICA_BOLD;
                        case "times", "times-roman", "times new roman", "serif" -> StandardFonts.TIMES_ROMAN;
                        case "times-bold", "times new roman bold", "serif-bold" -> StandardFonts.TIMES_BOLD;
                        case "courier", "monospace", "mono" -> StandardFonts.COURIER;
                        case "courier-bold", "mono-bold" -> StandardFonts.COURIER_BOLD;
                        default -> null;
                };

                if (standardFont == null) {
                        return fallback;
                }

                try {
                        return PdfFontFactory.createFont(standardFont, PdfEncodings.WINANSI);
                } catch (Exception e) {
                        log.warn("Template field font '{}' is not supported, fallback to default", fontFamily);
                        return fallback;
                }
        }

        private float normalizeTemplateFieldFontSize(Integer fontSize) {
                if (fontSize == null) {
                        return 11f;
                }
                if (fontSize < 6) {
                        return 6f;
                }
                if (fontSize > 72) {
                        return 72f;
                }
                return fontSize.floatValue();
        }

}

