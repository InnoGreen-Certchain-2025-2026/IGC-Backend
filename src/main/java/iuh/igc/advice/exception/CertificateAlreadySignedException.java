package iuh.igc.advice.exception;

public class CertificateAlreadySignedException extends RuntimeException {
    public CertificateAlreadySignedException(String message) {
        super(message);
    }
}
