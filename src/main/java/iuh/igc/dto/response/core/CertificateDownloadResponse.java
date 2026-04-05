package iuh.igc.dto.response.core;

public record CertificateDownloadResponse(
        String filename,
        byte[] bytes
) {}
