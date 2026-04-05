package iuh.igc.service.core;

import iuh.igc.dto.request.core.CreateDraftRequest;
import iuh.igc.dto.response.core.CertificateResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DraftCertificateService {

    @Transactional
    CertificateResponse createDraft(CreateDraftRequest request);

    List<CertificateResponse> getDraftCertificates();

    List<CertificateResponse> getSignedCertificates();

    List<CertificateResponse> getRevokedCertificates();

    @Transactional
    CertificateResponse reissueCertificate(String certificateId);
}
