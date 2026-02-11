package iuh.innogreen.blockchain.igc.service.core.impl;


import iuh.innogreen.blockchain.igc.dto.request.CertificateRequest;
import iuh.innogreen.blockchain.igc.dto.response.CertificateResponse;
import iuh.innogreen.blockchain.igc.dto.response.VerifyResponse;
import iuh.innogreen.blockchain.igc.entity.Certificate;
import iuh.innogreen.blockchain.igc.repository.CertificateRepository;
import iuh.innogreen.blockchain.igc.service.core.CertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final BlockchainServiceImpl blockchainServiceImpl;

    @Value("${blockchain.issuer-name}")
    private String issuerName;

    /**
     * Cấp chứng chỉ mới
     */
    @Transactional
    @Override
    public CertificateResponse issueCertificate(CertificateRequest request) {
        log.info("🎓 Issuing certificate: {}", request.getCertificateId());

        // Kiểm tra trùng lặp
        if (certificateRepository.existsByCertificateId(request.getCertificateId())) {
            throw new RuntimeException("Certificate ID already exists: " + request.getCertificateId());
        }

        try {
            // 1. Tạo hash từ thông tin chứng chỉ
            String documentHash = generateDocumentHash(request);
            log.info("📝 Generated hash: {}", documentHash);

            // 2. Lưu vào database
            Certificate certificate = Certificate.builder()
                    .certificateId(request.getCertificateId())
                    .studentName(request.getStudentName())
                    .studentId(request.getStudentId())
                    .dateOfBirth(request.getDateOfBirth())
                    .major(request.getMajor())
                    .graduationYear(request.getGraduationYear())
                    .gpa(request.getGpa())
                    .certificateType(request.getCertificateType())
                    .issuer(issuerName)
                    .issueDate(request.getIssueDate())
                    .documentHash(documentHash)
                    .isValid(true)
                    .build();

            certificate = certificateRepository.save(certificate);
            log.info("💾 Saved to database - ID: {}", certificate.getId());

            // 3. Ghi lên blockchain
            TransactionReceipt receipt = blockchainServiceImpl.issueCertificate(
                    request.getCertificateId(),
                    documentHash
            );

            // 4. Cập nhật thông tin blockchain
            certificate.setBlockchainTxHash(receipt.getTransactionHash());
            certificate.setBlockchainBlockNumber(receipt.getBlockNumber().longValue());

            // Lấy timestamp từ blockchain
            try {
                var block = blockchainServiceImpl.getWeb3j().ethGetBlockByNumber(
                        org.web3j.protocol.core.DefaultBlockParameter.valueOf(receipt.getBlockNumber()),
                        false
                ).send();

                if (block.getBlock() != null) {
                    certificate.setBlockchainTimestamp(
                            block.getBlock().getTimestamp().longValue()
                    );
                }
            } catch (Exception e) {
                log.warn("Could not retrieve block timestamp", e);
            }

            certificate = certificateRepository.save(certificate);

            log.info("✅ Certificate issued successfully!");
            log.info("   TX Hash: {}", certificate.getBlockchainTxHash());
            log.info("   Block: {}", certificate.getBlockchainBlockNumber());

            return mapToResponse(certificate);

        } catch (Exception e) {
            log.error("❌ Failed to issue certificate", e);
            throw new RuntimeException("Failed to issue certificate: " + e.getMessage(), e);
        }
    }

    /**
     * Xác thực chứng chỉ
     */
    @Override
    public VerifyResponse verifyCertificate(String certificateId) {
        log.info("🔍 Verifying certificate: {}", certificateId);

        // Kiểm tra database
        Certificate dbCert = certificateRepository.findByCertificateId(certificateId)
                .orElse(null);

        // Kiểm tra blockchain
        BlockchainServiceImpl.VerificationResult blockchainResult =
                blockchainServiceImpl.verifyCertificate(certificateId);

        if (dbCert == null) {
            return VerifyResponse.builder()
                    .exists(false)
                    .valid(false)
                    .certificateId(certificateId)
                    .message("Certificate not found in database")
                    .build();
        }

        if (!blockchainResult.getIsValid()) {
            return VerifyResponse.builder()
                    .exists(true)
                    .valid(false)
                    .certificateId(certificateId)
                    .studentName(dbCert.getStudentName())
                    .issuer(dbCert.getIssuer())
                    .message("Certificate not found on blockchain or has been revoked")
                    .build();
        }

        // So sánh hash
        boolean hashMatch = dbCert.getDocumentHash().equalsIgnoreCase(
                blockchainResult.getDocumentHash()
        );

        if (!hashMatch) {
            return VerifyResponse.builder()
                    .exists(true)
                    .valid(false)
                    .certificateId(certificateId)
                    .studentName(dbCert.getStudentName())
                    .issuer(dbCert.getIssuer())
                    .message("⚠️ WARNING: Certificate hash mismatch! Possible tampering detected!")
                    .build();
        }

        return VerifyResponse.builder()
                .exists(true)
                .valid(dbCert.getIsValid() && blockchainResult.getIsValid())
                .certificateId(dbCert.getCertificateId())
                .studentName(dbCert.getStudentName())
                .issuer(dbCert.getIssuer())
                .issueTimestamp(blockchainResult.getIssueTimestamp())
                .documentHash(dbCert.getDocumentHash())
                .message("✅ Certificate is valid and verified on blockchain")
                .build();
    }

    /**
     * Thu hồi chứng chỉ
     */
    @Transactional
    @Override
    public CertificateResponse revokeCertificate(String certificateId) {
        log.info("🚫 Revoking certificate: {}", certificateId);

        Certificate certificate = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found: " + certificateId));

        if (!certificate.getIsValid()) {
            throw new RuntimeException("Certificate already revoked");
        }

        // Thu hồi trên blockchain
        blockchainServiceImpl.revokeCertificate(certificateId);

        // Cập nhật database
        certificate.setIsValid(false);
        certificate = certificateRepository.save(certificate);

        log.info("✅ Certificate revoked successfully");

        return mapToResponse(certificate);
    }

    /**
     * Lấy tất cả chứng chỉ
     */
    @Override
    public List<CertificateResponse> getAllCertificates() {
        log.info("📋 Getting all certificates");
        return certificateRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chứng chỉ theo ID
     */
    @Override
    public CertificateResponse getCertificateById(String certificateId) {
        log.info("📄 Getting certificate: {}", certificateId);
        Certificate certificate = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found: " + certificateId));
        return mapToResponse(certificate);
    }

    /**
     * Tạo hash SHA-256
     */
    private String generateDocumentHash(CertificateRequest request) {
        try {
            String data = String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s",
                    request.getCertificateId(),
                    request.getStudentName(),
                    request.getStudentId(),
                    request.getDateOfBirth(),
                    request.getMajor(),
                    request.getGraduationYear(),
                    request.getGpa(),
                    request.getCertificateType(),
                    request.getIssueDate()
            );

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder("0x");
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate hash", e);
        }
    }

    /**
     * Map Entity to Response
     */
    private CertificateResponse mapToResponse(Certificate certificate) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .certificateId(certificate.getCertificateId())
                .studentName(certificate.getStudentName())
                .studentId(certificate.getStudentId())
                .dateOfBirth(certificate.getDateOfBirth())
                .major(certificate.getMajor())
                .graduationYear(certificate.getGraduationYear())
                .gpa(certificate.getGpa())
                .certificateType(certificate.getCertificateType())
                .issuer(certificate.getIssuer())
                .issueDate(certificate.getIssueDate())
                .documentHash(certificate.getDocumentHash())
                .blockchainTxHash(certificate.getBlockchainTxHash())
                .blockchainBlockNumber(certificate.getBlockchainBlockNumber())
                .blockchainTimestamp(certificate.getBlockchainTimestamp())
                .isValid(certificate.getIsValid())
                .createdAt(certificate.getCreatedAt())
                .build();
    }
}
