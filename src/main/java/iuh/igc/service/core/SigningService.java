package iuh.igc.service.core;

import iuh.igc.dto.request.core.SignCertificateRequest;
import iuh.igc.dto.response.core.CertificateResponse;
import org.springframework.transaction.annotation.Transactional;

public interface SigningService {

    @Transactional
    CertificateResponse signCertificate(String certificateId, SignCertificateRequest request);

    @Transactional
    CertificateResponse revokeCertificate(String certificateId);
}
