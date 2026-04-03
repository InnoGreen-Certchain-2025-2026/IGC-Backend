package iuh.igc.dto.request.core;

import org.springframework.web.multipart.MultipartFile;

public record SignCertificateRequest(
        MultipartFile signatureImage,
        MultipartFile userCertificate,
        String certificatePassword,
        SignaturePosition signaturePosition
) {
}
