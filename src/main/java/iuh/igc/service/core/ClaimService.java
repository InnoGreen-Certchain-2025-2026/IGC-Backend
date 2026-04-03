package iuh.igc.service.core;

import iuh.igc.dto.response.core.CertificateDownloadResponse;
import iuh.igc.dto.response.core.CertificateResponse;

public interface ClaimService {

    CertificateResponse claimCertificate(String claimCode);

    CertificateResponse getCertificateByClaimCode(String claimCode);

    CertificateDownloadResponse downloadCertificateByClaimCode(String claimCode);
}
