package iuh.igc.controller;

import iuh.igc.dto.base.ApiResponse;
import iuh.igc.entity.Signature;
import iuh.igc.entity.organization.Organization;
import iuh.igc.repository.OrganizationRepository;
import iuh.igc.repository.SignatureRepository;
import iuh.igc.service.opencv.SignatureService;
import iuh.igc.service.pdf.HashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/signature")
public class SignatureController {

    @Autowired
    private SignatureService signatureService;
    @Autowired
    private HashService hashService;
    @Autowired
    private SignatureRepository signatureRepository;
    @Autowired
    private OrganizationRepository organizationRepository;

    @PostMapping("/check")
    public ApiResponse<Map<String,Object>> checkSignature(
            @RequestParam Long orgId,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        try {
            boolean isSignature = signatureService.isSignature(file);
            if (!isSignature) {
                throw new IllegalArgumentException("Uploaded file is not a valid signature");
            }

            String hash = hashService.hashBytes(file.getBytes());
            boolean isUsed = signatureRepository.existsByOrganizationIdAndHash(orgId, hash);

            // Trả về front, front tự quyết định
            Map<String, Object> data = new HashMap<>();
            data.put("hash", hash);
            data.put("isUsed", isUsed); // true → front hiện cảnh báo

            return new ApiResponse<>(data);

        } catch (IOException e) {
            throw new RuntimeException("Error processing file", e);
        }
    }

    // Front xác nhận rồi mới gọi, chỉ nhận hash
    @PostMapping("/confirm")
    public ApiResponse<Boolean> confirmSignature(
            @RequestParam Long orgId,
            @RequestParam String hash) {

        //Xóa hash củ nếu trùng
        if (signatureRepository.existsByOrganizationIdAndHash(orgId, hash)) {
            signatureRepository.removeByOrganizationIdAndHash(orgId,hash);
        }
        // Deactivate chữ ký cũ
        List<Signature> oldSignatures = signatureRepository
                .findByOrganizationIdAndIsActiveTrue(orgId);
        oldSignatures.forEach(s -> s.setActive(false));
        signatureRepository.saveAll(oldSignatures);

        // Lưu chữ ký mới
        Signature newSignature = Signature.builder()
                .hash(hash)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .organization(organizationRepository.getReferenceById(orgId))
                .build();
        signatureRepository.save(newSignature);

        return new ApiResponse<>(true);
    }
}
