package iuh.igc.service.pdf;

public interface DigitalSignatureService {
    byte[] signPdfWithUserCertificate(byte[] pdfBytes, byte[] pkcs12Bytes, String password);

    boolean verifyPdfSignature(byte[] signedPdfBytes);
}
