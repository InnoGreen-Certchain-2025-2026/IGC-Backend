package iuh.igc.service.pdf;

import jakarta.annotation.PostConstruct;

public interface DigitalSignatureService {
    @PostConstruct
    void init();

    byte[] signPdf(byte[] pdfBytes, String certificateId);

    boolean verifyPdfSignature(byte[] signedPdfBytes);
}
