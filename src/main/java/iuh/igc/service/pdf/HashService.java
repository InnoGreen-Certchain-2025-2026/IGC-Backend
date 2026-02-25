package iuh.igc.service.pdf;

public interface HashService {
    String hashBytes(byte[] data);

    String hashString(String data);

    boolean verifyHash(byte[] data, String expectedHash);
}
