package iuh.igc.service.pdf;

import jakarta.annotation.PostConstruct;

public interface DigitalSignatureService {
    @PostConstruct
    void init();

    byte[] signPdf(byte[] pdfBytes, String certificateId);

    byte[] signPdfWithUserCertificate(byte[] pdfBytes, byte[] pkcs12Bytes, String password);

    boolean verifyPdfSignature(byte[] signedPdfBytes);
}
