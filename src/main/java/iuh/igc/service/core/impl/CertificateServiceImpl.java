package iuh.igc.service.core.impl;

import iuh.igc.config.s3.S3Service;
import iuh.igc.dto.request.core.CertificateRequest;
import iuh.igc.dto.response.core.CertificateDownloadResponse;
import iuh.igc.dto.response.core.CertificateResponse;
import iuh.igc.dto.response.core.VerifyResponse;
import iuh.igc.entity.Certificate;
import iuh.igc.entity.User;
import iuh.igc.entity.constant.CertificateStatus;
import iuh.igc.entity.organization.Organization;
import iuh.igc.repository.CertificateRepository;
import iuh.igc.repository.OrganizationMemberRepository;
import iuh.igc.repository.OrganizationRepository;
import iuh.igc.service.core.BlockchainService;
import iuh.igc.service.core.CertificateService;
import iuh.igc.service.pdf.DigitalSignatureService;
import iuh.igc.service.pdf.HashService;
import iuh.igc.service.pdf.PdfService;
import iuh.igc.service.user.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final BlockchainService blockchainService;
    private final PdfService pdfService;
    private final DigitalSignatureService signatureService;
    private final HashService hashService;
    private final S3Service s3Service;
    private final OrganizationRepository organizationRepository;

    private final CurrentUserProvider currentUserProvider;

    @Value("${blockchain.issuer-name}")
    private String issuerName;

    @Value("${aws.s3.domain}")
    private String domain;

    /**
     * LUỒNG CHÍNH: Cấp chứng chỉ với PDF ký số
     */
    @Override
    @Transactional
    public CertificateResponse issueCertificate(CertificateRequest request, Long organizationId) {
        throw new UnsupportedOperationException("Use multipart issueCertificate(request, orgId, userCertificate, certificatePassword) to sign with user certificate");
        }

        @Override
        @Transactional
        public CertificateResponse issueCertificate(
            CertificateRequest request,
            Long organizationId,
            MultipartFile userCertificate,
            String certificatePassword) {
        User user = currentUserProvider.get();

        Organization organization = organizationRepository
                .findByIdAndOrganizationMembers_User_Id(organizationId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tổ chức"));

        log.info("🎓 ========== ISSUING NEW CERTIFICATE ==========");
        log.info("Certificate ID: {}", request.certificateId());
        log.info("Student: {}", request.studentName());
        log.info("Vendor: {}", organization.getName());

        // Kiểm tra trùng lặp
        if (certificateRepository.existsByCertificateId(request.certificateId())) {
            throw new RuntimeException("Certificate ID already exists: " + request.certificateId());
        }

        validateUserCertificateInput(userCertificate, certificatePassword);



        try {
            // ========== STEP 1: Generate PDF ==========
            log.info("📄 Step 1: Generating PDF template...");
            byte[] unsignedPdf = pdfService.generateCertificatePdf(request, organization.getName());
            log.info("✅ PDF generated - Size: {} bytes", unsignedPdf.length);

            // ========== STEP 2: Digital Signature ==========
            log.info("✍️ Step 2: Signing PDF with user certificate...");
            byte[] signedPdf = signatureService.signPdfWithUserCertificate(
                    unsignedPdf,
                    userCertificate.getBytes(),
                    certificatePassword
            );
            log.info("✅ PDF signed - Size: {} bytes", signedPdf.length);

            // ========== STEP 3: Hash Signed PDF ==========
            log.info("🔐 Step 3: Hashing signed PDF...");
            String signedPdfHash = hashService.hashBytes(signedPdf);
            log.info("✅ Hash generated: {}", signedPdfHash);

            // ========== STEP 4: Upload to S3 ==========
            log.info("☁️ Step 4: Uploading PDF to S3...");

            // Tạo folder path theo năm
            String folderName = "certificates/" + request.issueDate().getYear();

            // Tạo filename với certificateId
            String filename = request.certificateId() + ".pdf";

            // Convert byte[] thành MultipartFile để upload
            MultipartFile multipartFile = new MockMultipartFile(
                    filename,
                    filename,
                    "application/pdf",
                    signedPdf
            );

            // Upload và lấy URL (getUrl = true)
            String s3Url = s3Service.uploadFile(
                    multipartFile,
                    folderName,
                    true,  // getUrl = true để lấy full URL
                    10 * 1024 * 1024  // maxFileSize = 10MB
            );

            String s3Key = s3Url.replace(domain + "/", "");

            log.info("✅ PDF uploaded to S3");
            log.info("   URL: {}", s3Url);
            log.info("   Key: {}", s3Key);

            // ========== STEP 5: Save to Database ==========
            log.info("💾 Step 5: Saving to database...");


            Certificate certificate = Certificate.builder()
                    .certificateId(request.certificateId())
                    .studentName(request.studentName())
//                    .studentId(request.studentId())
                    .dateOfBirth(request.dateOfBirth())
                    .major(request.major())
                    .graduationYear(request.graduationYear())
                    .gpa(request.gpa())
                    .certificateType(request.certificateType())
                    .issuer(organization.getCode())
                    .issueDate(request.issueDate())
                    .pdfFilename(filename)
                    .pdfS3Path(s3Key)
                    .signedPdfS3Path(s3Key)
                    .pdfS3Url(s3Url)
                    .pdfSizeBytes((long) signedPdf.length)
                    .signedPdfHash(signedPdfHash)
                    .signatureTimestamp(LocalDateTime.now())
                    .signerName(user.getName())
                    .isValid(true)
                    .status(CertificateStatus.SIGNED)
                    .claimCode(generateClaimCode(organization.getCode()))
                    .claimCodeExpiresAt(LocalDateTime.now().plusDays(30))
                    .isClaim(false)
                    .build();

            certificate = certificateRepository.save(certificate);



            log.info("✅ Saved to database - ID: {}", certificate.getId());

            // ========== STEP 6: Write to Blockchain ==========
            log.info("⛓️ Step 6: Writing hash to blockchain...");
            TransactionReceipt receipt = blockchainService.issueCertificate(
                    request.certificateId(),
                    signedPdfHash
            );

            // ========== STEP 7: Update Blockchain Info ==========
            log.info("📝 Step 7: Updating blockchain info...");
            certificate.setBlockchainTxHash(receipt.getTransactionHash());
            certificate.setBlockchainBlockNumber(receipt.getBlockNumber().longValue());

            // Get timestamp from blockchain
            try {
                var block = blockchainService.getWeb3j().ethGetBlockByNumber(
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

            log.info("🎉 ========== CERTIFICATE ISSUED SUCCESSFULLY ==========");
            log.info("   Database ID: {}", certificate.getId());
            log.info("   TX Hash: {}", certificate.getBlockchainTxHash());
            log.info("   Block: {}", certificate.getBlockchainBlockNumber());
            log.info("   PDF URL: {}", certificate.getPdfS3Url());
            log.info("   PDF Key: {}", certificate.getPdfS3Path());
            log.info("   Hash: {}", certificate.getSignedPdfHash());
            log.info("====================================================");

            return mapToResponse(certificate);

        } catch (Exception e) {
            log.error("❌ Failed to issue certificate", e);
            throw new RuntimeException("Failed to issue certificate: " + e.getMessage(), e);
        }
    }

    private void validateUserCertificateInput(MultipartFile userCertificate, String certificatePassword) {
        if (userCertificate == null || userCertificate.isEmpty()) {
            throw new IllegalArgumentException("User certificate is required");
        }

        if (certificatePassword == null || certificatePassword.isBlank()) {
            throw new IllegalArgumentException("Certificate password is required");
        }

        String certName = userCertificate.getOriginalFilename();
        if (certName == null || !(certName.toLowerCase().endsWith(".p12") || certName.toLowerCase().endsWith(".pfx"))) {
            throw new IllegalArgumentException("User certificate must be a .p12 or .pfx file");
        }
    }

    /**
     * Xác thực chứng chỉ bằng cách download PDF và so sánh hash
     */
    @Override
    public VerifyResponse verifyCertificate(String certificateId) {
        log.info("🔍 ========== VERIFYING CERTIFICATE ==========");
        log.info("Certificate ID: {}", certificateId);

        try {
            // ========== STEP 1: Check Database ==========
            log.info("📋 Step 1: Checking database...");
            Certificate dbCert = certificateRepository.findByCertificateId(certificateId)
                    .orElse(null);

            if (dbCert == null) {
                log.warn("❌ Certificate not found in database");
                return VerifyResponse.builder()
                        .exists(false)
                        .valid(false)
                        .certificateId(certificateId)
                        .message("Certificate not found in database")
                        .build();
            }

            log.info("✅ Found in database");

            // ========== STEP 2: Check Blockchain ==========
            log.info("⛓️ Step 2: Checking blockchain...");
            BlockchainService.VerificationResult blockchainResult =
                    blockchainService.verifyCertificate(certificateId);

            if (blockchainResult == null || !blockchainResult.getIsValid()) {
                log.warn("❌ Certificate not found or revoked on blockchain");
                return VerifyResponse.builder()
                        .exists(true)
                        .valid(false)
                        .certificateId(certificateId)
                        .studentName(dbCert.getStudentName())
                        .issuer(dbCert.getIssuer())
                        .message("Certificate not found on blockchain or has been revoked")
                        .build();
            }

            log.info("✅ Found on blockchain");

            // ========== STEP 3: Download PDF from S3 ==========
            log.info("☁️ Step 3: Downloading PDF from S3...");

            // Download PDF bằng S3 key
            byte[] pdfBytes = s3Service.downloadFileAsBytes(dbCert.getPdfS3Path());

            log.info("✅ PDF downloaded - Size: {} bytes", pdfBytes.length);

            // ========== STEP 4: Hash Downloaded PDF ==========
            log.info("🔐 Step 4: Hashing downloaded PDF...");
            String downloadedPdfHash = hashService.hashBytes(pdfBytes);
            log.info("   Downloaded PDF hash: {}", downloadedPdfHash);

            // ========== STEP 5: Compare Hashes ==========
            log.info("🔍 Step 5: Comparing hashes...");
            log.info("   Database hash:   {}", dbCert.getSignedPdfHash());
            log.info("   Blockchain hash: {}", blockchainResult.getDocumentHash());
            log.info("   PDF hash:        {}", downloadedPdfHash);

            boolean databaseMatch = dbCert.getSignedPdfHash().equalsIgnoreCase(downloadedPdfHash);
            boolean blockchainMatch = blockchainResult.getDocumentHash().equalsIgnoreCase(downloadedPdfHash);

            if (!databaseMatch || !blockchainMatch) {
                log.error("❌ HASH MISMATCH DETECTED!");
                log.error("   Database match: {}", databaseMatch);
                log.error("   Blockchain match: {}", blockchainMatch);

                return VerifyResponse.builder()
                        .exists(true)
                        .valid(false)
                        .certificateId(certificateId)
                        .studentName(dbCert.getStudentName())
                        .issuer(dbCert.getIssuer())
                        .documentHash(downloadedPdfHash)
                        .message("⚠️ CRITICAL: Hash mismatch detected! Certificate may have been tampered with!")
                        .build();
            }

            // ========== STEP 6: Verify Digital Signature ==========
            log.info("✍️ Step 6: Verifying digital signature...");
            boolean signatureValid = signatureService.verifyPdfSignature(pdfBytes);

            if (!signatureValid) {
                log.warn("⚠️ Digital signature verification failed, but hash/blockchain verification passed");
                return VerifyResponse.builder()
                    .exists(true)
                    .valid(true)
                    .certificateId(dbCert.getCertificateId())
                    .studentName(dbCert.getStudentName())
                    .issuer(dbCert.getIssuer())
                    .issueTimestamp(blockchainResult.getIssueTimestamp())
                    .documentHash(downloadedPdfHash)
                    .message("Certificate hash is valid on database/blockchain, but digital signature could not be fully verified")
                    .build();
            }

            log.info("✅ Digital signature verified");

            // ========== ALL CHECKS PASSED ==========
            log.info("🎉 ========== VERIFICATION SUCCESSFUL ==========");
            log.info("   ✅ Database: Found");
            log.info("   ✅ Blockchain: Valid");
            log.info("   ✅ Hash: Match");
            log.info("   ✅ Signature: Valid");
            log.info("================================================");

            return VerifyResponse.builder()
                    .exists(true)
                    .valid(true)
                    .certificateId(dbCert.getCertificateId())
                    .studentName(dbCert.getStudentName())
                    .issuer(dbCert.getIssuer())
                    .issueTimestamp(blockchainResult.getIssueTimestamp())
                    .documentHash(downloadedPdfHash)
                    .message("✅ Certificate is AUTHENTIC and VALID! All verification checks passed.")
                    .build();

        } catch (Exception e) {
            log.error("❌ Verification failed with error", e);
            return VerifyResponse.builder()
                    .exists(false)
                    .valid(false)
                    .certificateId(certificateId)
                    .message("Verification failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xác thực chứng chỉ bằng cách upload file PDF
     * Client upload file PDF → Hash file → So sánh với blockchain
     */
    @Override
    public VerifyResponse verifyCertificateByFile(MultipartFile pdfFile) {
        log.info("🔍 ========== VERIFYING CERTIFICATE BY FILE ==========");

        try {
            // ========== STEP 1: Validate File ==========
            log.info("📄 Step 1: Validating uploaded file...");

            if (pdfFile == null || pdfFile.isEmpty()) {
                throw new RuntimeException("PDF file is required");
            }

            if (!"application/pdf".equals(pdfFile.getContentType())) {
                throw new RuntimeException("File must be a PDF");
            }

            log.info("   File name: {}", pdfFile.getOriginalFilename());
            log.info("   File size: {} bytes", pdfFile.getSize());

            // ========== STEP 2: Read PDF Bytes ==========
            log.info("📖 Step 2: Reading PDF content...");
            byte[] pdfBytes = pdfFile.getBytes();
            log.info("✅ PDF read - Size: {} bytes", pdfBytes.length);

            // ========== STEP 3: Hash Uploaded PDF ==========
            log.info("🔐 Step 3: Hashing uploaded PDF...");
            String uploadedPdfHash = hashService.hashBytes(pdfBytes);
            log.info("   Uploaded PDF hash: {}", uploadedPdfHash);

            // ========== STEP 4: Find Certificate by Hash in Database ==========
            log.info("🔍 Step 4: Searching certificate by hash in database...");
            Certificate dbCert = certificateRepository.findBySignedPdfHash(uploadedPdfHash)
                    .orElse(null);

            if (dbCert == null) {
                log.warn("❌ No certificate found with this hash in database");
                return VerifyResponse.builder()
                        .exists(false)
                        .valid(false)
                        .documentHash(uploadedPdfHash)
                        .message("No certificate found matching this PDF file")
                        .build();
            }

            log.info("✅ Found certificate in database: {}", dbCert.getCertificateId());

            // ========== STEP 5: Check Blockchain ==========
            log.info("⛓️ Step 5: Verifying on blockchain...");
            BlockchainService.VerificationResult blockchainResult =
                    blockchainService.verifyCertificate(dbCert.getCertificateId());

            if (blockchainResult == null || !blockchainResult.getIsValid()) {
                log.warn("❌ Certificate not found or revoked on blockchain");
                return VerifyResponse.builder()
                        .exists(true)
                        .valid(false)
                        .certificateId(dbCert.getCertificateId())
                        .studentName(dbCert.getStudentName())
                        .issuer(dbCert.getIssuer())
                        .documentHash(uploadedPdfHash)
                        .message("Certificate has been REVOKED on blockchain")
                        .build();
            }

            log.info("✅ Certificate is valid on blockchain");

            // ========== STEP 6: Compare Hashes ==========
            log.info("🔍 Step 6: Comparing hashes...");
            log.info("   Database hash:   {}", dbCert.getSignedPdfHash());
            log.info("   Blockchain hash: {}", blockchainResult.getDocumentHash());
            log.info("   Uploaded hash:   {}", uploadedPdfHash);

            boolean databaseMatch = dbCert.getSignedPdfHash().equalsIgnoreCase(uploadedPdfHash);
            boolean blockchainMatch = blockchainResult.getDocumentHash().equalsIgnoreCase(uploadedPdfHash);

            if (!databaseMatch || !blockchainMatch) {
                log.error("❌ HASH MISMATCH DETECTED!");
                log.error("   Database match: {}", databaseMatch);
                log.error("   Blockchain match: {}", blockchainMatch);

                return VerifyResponse.builder()
                        .exists(true)
                        .valid(false)
                        .certificateId(dbCert.getCertificateId())
                        .studentName(dbCert.getStudentName())
                        .issuer(dbCert.getIssuer())
                        .documentHash(uploadedPdfHash)
                        .message("⚠️ CRITICAL: Hash mismatch! The PDF file has been TAMPERED or MODIFIED!")
                        .build();
            }

            log.info("✅ Hash verification passed");

            // ========== STEP 7: Verify Digital Signature ==========
            log.info("✍️ Step 7: Verifying digital signature...");
            boolean signatureValid = signatureService.verifyPdfSignature(pdfBytes);

            if (!signatureValid) {
                log.warn("⚠️ Digital signature verification failed, but hash/blockchain verification passed");
                return VerifyResponse.builder()
                    .exists(true)
                    .valid(true)
                    .certificateId(dbCert.getCertificateId())
                    .studentName(dbCert.getStudentName())
                    .issuer(dbCert.getIssuer())
                    .issueTimestamp(blockchainResult.getIssueTimestamp())
                    .documentHash(uploadedPdfHash)
                    .message("Certificate hash is valid on database/blockchain, but digital signature could not be fully verified")
                    .build();
            }

            log.info("✅ Digital signature verified");

            // ========== STEP 8: Check Certificate Validity Status ==========
            if (!dbCert.getIsValid()) {
                log.warn("⚠️ Certificate is marked as revoked in database");
                return VerifyResponse.builder()
                        .exists(true)
                        .valid(false)
                        .certificateId(dbCert.getCertificateId())
                        .studentName(dbCert.getStudentName())
                        .issuer(dbCert.getIssuer())
                        .issueTimestamp(blockchainResult.getIssueTimestamp())
                        .documentHash(uploadedPdfHash)
                        .message("Certificate has been REVOKED")
                        .build();
            }

            // ========== ALL CHECKS PASSED ==========
            log.info("🎉 ========== VERIFICATION SUCCESSFUL ==========");
            log.info("   ✅ File: Valid PDF");
            log.info("   ✅ Hash: Match");
            log.info("   ✅ Database: Found");
            log.info("   ✅ Blockchain: Valid");
            log.info("   ✅ Digital Signature: Valid");
            log.info("   ✅ Status: Active");
            log.info("================================================");

            return VerifyResponse.builder()
                    .exists(true)
                    .valid(true)
                    .certificateId(dbCert.getCertificateId())
                    .studentName(dbCert.getStudentName())
                    .issuer(dbCert.getIssuer())
                    .issueTimestamp(blockchainResult.getIssueTimestamp())
                    .documentHash(uploadedPdfHash)
                    .message("✅ Certificate is AUTHENTIC and VALID! All verification checks passed.")
                    .build();

        } catch (Exception e) {
            log.error("❌ Verification failed with error", e);
            return VerifyResponse.builder()
                    .exists(false)
                    .valid(false)
                    .message("Verification failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Download PDF certificate
     */
    @Override
    public byte[] downloadCertificatePdf(String certificateId) {
        log.info("📥 Downloading PDF for certificate: {}", certificateId);

        Certificate certificate = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));

        try {
            // Download từ S3 bằng key
            byte[] pdfBytes = s3Service.downloadFileAsBytes(certificate.getPdfS3Path());

            log.info("✅ PDF downloaded - Size: {} bytes", pdfBytes.length);
            return pdfBytes;

        } catch (Exception e) {
            log.error("❌ Failed to download PDF", e);
            throw new RuntimeException("PDF download failed: " + e.getMessage(), e);
        }
    }

    /**
     * Thu hồi chứng chỉ
     */
    @Override
    @Transactional
    public CertificateResponse revokeCertificate(String certificateId) {
        log.info("🚫 Revoking certificate: {}", certificateId);

        Certificate certificate = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found: " + certificateId));

        if (!certificate.getIsValid()) {
            throw new RuntimeException("Certificate already revoked");
        }

        // Thu hồi trên blockchain
        blockchainService.revokeCertificate(certificateId);

        // Cập nhật database
        certificate.setIsValid(false);
        certificate.setStatus(CertificateStatus.REVOKED);
        certificate = certificateRepository.save(certificate);

        log.info("✅ Certificate revoked successfully");

        return mapToResponse(certificate);
    }

    @Override
    @Transactional
    public CertificateResponse reactivateCertificate(String certificateId) {
        log.info("♻️ Reactivating certificate: {}", certificateId);

        Certificate certificate = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found: " + certificateId));

        if (certificate.getIsValid()) {
            throw new RuntimeException("Certificate is already active");
        }

        // Reactivate trên blockchain
        blockchainService.reactivateCertificate(certificateId);

        // Cập nhật database
        certificate.setIsValid(true);
        certificate.setStatus(CertificateStatus.SIGNED);
        certificate = certificateRepository.save(certificate);

        log.info("✅ Certificate reactivated successfully");

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

    @Override
    public List<CertificateResponse> getCertificatesByOrganizationId(Long organizationId) {
        log.info("📋 Getting certificates for organization ID: {}", organizationId);
        return certificateRepository.findAll().stream()
                .filter(cert -> cert.getIssuer().equals(organizationRepository.findById(organizationId)
                        .orElseThrow(() -> new RuntimeException("Organization not found"))
                        .getCode()))
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

    @Override
    public String generateClaimCode(String organizationCode) {
        String claimCode;
        do {
            int number = ThreadLocalRandom.current().nextInt(100000, 1000000);
            claimCode = organizationCode + "-" + number;
        } while (certificateRepository.existsByClaimCode(claimCode));

        return claimCode;
    }

    @Override
    @Transactional
    public CertificateResponse getCertificateByClaimCode(String claimCode) {
        User user = currentUserProvider.get();
        Certificate certificate = certificateRepository
                .findByClaimCode(claimCode)
                .orElseThrow(() -> new RuntimeException("Invalid claim code"));

        if (Boolean.TRUE.equals(certificate.getIsClaim())) {
            throw new RuntimeException("Certificate has already been claimed");
        }

        certificate.setStudentId(user.getId());
        certificate.setIsClaim(true);

        certificate = certificateRepository.save(certificate);

        log.info("Certificate claimed successfully by studentId: {}", user.getId());

        return mapToResponse(certificate);
    }

    @Override
    public List<CertificateResponse> getAllCertificatesByStudentId() {
        User user = currentUserProvider.get();
        return certificateRepository.findCertificateByStudentId(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CertificateResponse> getSignedCertificates() {
        return certificateRepository.findByStatus(CertificateStatus.SIGNED)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<CertificateResponse> getRevokedCertificates() {
        return certificateRepository.findByStatus(CertificateStatus.REVOKED)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public CertificateDownloadResponse downloadCertificate(String certificateId) {

        User user = currentUserProvider.get();

        Certificate certificate = certificateRepository
                .findByCertificateId(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));

        if (!certificate.getStudentId().equals(user.getId())) {
            throw new RuntimeException("You are not allowed to download this certificate");
        }

        byte[] pdfBytes = s3Service.downloadFileAsBytes(
                certificate.getPdfS3Path()
        );

        return new CertificateDownloadResponse(
                certificate.getPdfFilename(),
                pdfBytes
        );
    }
}