package iuh.innogreen.blockchain.igc.service.core;

import iuh.innogreen.blockchain.igc.dto.request.CertificateRequest;
import iuh.innogreen.blockchain.igc.dto.response.CertificateResponse;
import iuh.innogreen.blockchain.igc.dto.response.VerifyResponse;
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
