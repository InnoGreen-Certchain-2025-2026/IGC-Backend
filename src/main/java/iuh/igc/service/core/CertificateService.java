package iuh.igc.service.core;

import iuh.igc.dto.request.core.CertificateRequest;
import iuh.igc.dto.response.core.CertificateResponse;
import iuh.igc.dto.response.core.VerifyResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin 2/11/2026
 *
 **/
public interface CertificateService {
    @Transactional
    CertificateResponse issueCertificate(CertificateRequest request);

    VerifyResponse verifyCertificate(String certificateId);

    @Transactional
    CertificateResponse revokeCertificate(String certificateId);

    List<CertificateResponse> getAllCertificates();

    CertificateResponse getCertificateById(String certificateId);
}
