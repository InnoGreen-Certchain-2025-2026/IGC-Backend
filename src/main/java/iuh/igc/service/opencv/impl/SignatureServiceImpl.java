package iuh.igc.service.opencv.impl;

import iuh.igc.config.s3.S3Service;
import iuh.igc.entity.Signature;
import iuh.igc.repository.OrganizationRepository;
import iuh.igc.repository.SignatureRepository;
import iuh.igc.service.opencv.SignatureService;
import iuh.igc.service.pdf.HashService;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SignatureServiceImpl implements SignatureService {

    @Autowired
    private HashService hashService;
    @Autowired
    private SignatureRepository signatureRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private S3Service s3Service;

    @Override
    public boolean isSignature(MultipartFile file) {
        try {
            // Convert MultipartFile -> byte[] -> Mat
            byte[] bytes = file.getBytes();
            Mat img = Imgcodecs.imdecode(
                    new MatOfByte(bytes),
                    Imgcodecs.IMREAD_COLOR
            );

            if (img.empty()) {
                throw new RuntimeException("Không đọc được ảnh");
            }

            // 1. grayscale
            Mat gray = new Mat();
            Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);

            double meanIntensity = Core.mean(gray).val[0];
            if(meanIntensity >200){
                // 2. blur
                Mat blurred = new Mat();
                Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

                // 3. threshold
                Mat binary = new Mat();
                Imgproc.threshold(blurred, binary, 0, 255,
                        Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

                // 4. find contours
                List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(binary, contours, hierarchy,
                        Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                // 5. tính diện tích
                double totalArea = 0;
                for (MatOfPoint contour : contours) {
                    totalArea += Imgproc.contourArea(contour);
                }

                double imageArea = binary.rows() * binary.cols();
                double ratio = totalArea / imageArea;

                return ratio > 0.01 && ratio < 0.2;
            }
            return false;

        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file: " + e.getMessage());
        }
    }

    @Transactional
    public boolean createSignature(Long orgId, MultipartFile file) {
        String hash;
        try {
            hash = hashService.hashBytes(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Error processing file", e);
        }
        Signature oldSignatures = signatureRepository
                .findByOrganizationIdAndIsActiveTrue(orgId);

// 1. Nếu đã tồn tại → skip
        if (oldSignatures != null && oldSignatures.getHash().equals(hash)) {
            return true;
        }

// 2. Nếu có chữ ký cũ → deactivate
        if (oldSignatures != null) {
            oldSignatures.setActive(false);
            signatureRepository.save(oldSignatures);
        }

        try {
            Signature newSignature = Signature.builder()
                    .hash(hash)
                    .isActive(true)
                    .createdAt(java.time.LocalDateTime.now())
                    .organization(organizationRepository.getReferenceById(orgId))
                    .build();

            //Lưu file lên S3
            String key = "signatures/" + orgId + "/" + hash + ".png";
            s3Service.uploadFile(file, key, false, 5 * 1024 * 1024L);

            signatureRepository.save(newSignature);


            return true;
        } catch (Exception e) {
            throw new RuntimeException("Error saving signature: " + e.getMessage(), e);
        }
    }

    public boolean checkSignatureIsUsed(MultipartFile file, Long orgId) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (!isSignature(file)) {
            throw new IllegalArgumentException("Uploaded file is not a valid signature");
        }

        try {
            String hash = hashService.hashBytes(file.getBytes());
            return signatureRepository.existsByOrganizationIdAndHash(orgId, hash);

        } catch (IOException e) {
            throw new RuntimeException("Error processing file", e);
        }
    }
}
