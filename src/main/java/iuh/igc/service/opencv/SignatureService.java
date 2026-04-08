package iuh.igc.service.opencv;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


public interface SignatureService {
    public boolean isSignature(MultipartFile file);
    public boolean createSignature(Long orgId, MultipartFile file);
    public boolean checkSignatureIsUsed(MultipartFile file, Long orgId);
}
