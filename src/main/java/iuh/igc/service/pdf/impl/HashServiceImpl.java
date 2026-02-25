package iuh.igc.service.pdf.impl;

import iuh.igc.service.pdf.HashService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class HashServiceImpl implements HashService {
    /**
     * Tạo SHA-256 hash từ byte array (PDF)
     */
    @Override
    public String hashBytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);

            StringBuilder hexString = new StringBuilder("0x");
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String hash = hexString.toString();
            log.debug("Generated hash: {}", hash);

            return hash;

        } catch (Exception e) {
            log.error("❌ Failed to generate hash", e);
            throw new RuntimeException("Hash generation failed", e);
        }
    }

    /**
     * Tạo SHA-256 hash từ string
     */
    @Override
    public String hashString(String data) {
        return hashBytes(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Verify hash
     */
    @Override
    public boolean verifyHash(byte[] data, String expectedHash) {
        String actualHash = hashBytes(data);
        boolean match = actualHash.equalsIgnoreCase(expectedHash);

        log.debug("Hash verification: {}", match ? "✅ Match" : "❌ Mismatch");
        log.debug("  Expected: {}", expectedHash);
        log.debug("  Actual:   {}", actualHash);

        return match;
    }
}
