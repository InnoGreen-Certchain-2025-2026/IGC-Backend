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
import java.util.Comparator;
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
            byte[] bytes = file.getBytes();
            Mat img = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);
            if (img.empty()) throw new RuntimeException("Không đọc được ảnh");

            // 1. Grayscale
            Mat gray = new Mat();
            Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);

            double meanIntensity = Core.mean(gray).val[0];
            if (meanIntensity < 160) return false;

            // 2. Normalize + Blur + Threshold
            Mat normalized = new Mat();
            Core.normalize(gray, normalized, 0, 255, Core.NORM_MINMAX);

            Mat blurred = new Mat();
            Imgproc.GaussianBlur(normalized, blurred, new Size(5, 5), 0);

            Mat binary = new Mat();
            Imgproc.threshold(blurred, binary, 0, 255,
                    Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

            // 3. Contours
            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.findContours(binary, contours, new Mat(),
                    Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            if (contours.isEmpty()) return false;

            double imageArea   = binary.rows() * binary.cols();
            double imageHeight = binary.rows();

            // =====================================================
            // FILTER 1: Y-histogram — đo độ ĐỀU ĐẶN phân bố theo chiều dọc
            // Text in: đều đặn (bucketCV thấp, gapCV thấp)
            // Chữ ký dài: ngẫu nhiên (bucketCV cao, gapCV cao)
            // =====================================================
            int yBuckets = 20;
            int[] yHistogram = new int[yBuckets];
            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area < 5) continue;
                Rect r = Imgproc.boundingRect(contour);
                int bucket = (int) ((r.y + r.height / 2.0) / imageHeight * yBuckets);
                bucket = Math.min(bucket, yBuckets - 1);
                yHistogram[bucket]++;
            }

            List<Integer> occupiedYList = new ArrayList<>();
            int totalInBuckets = 0;
            for (int i = 0; i < yBuckets; i++) {
                if (yHistogram[i] > 2) {
                    occupiedYList.add(i);
                    totalInBuckets += yHistogram[i];
                }
            }

            int occupiedBuckets = occupiedYList.size();

            if (occupiedBuckets > 4) {
                // Đo độ đều của số contour trong mỗi bucket
                double meanPerBucket = (double) totalInBuckets / occupiedBuckets;
                double bucketVariance = 0;
                for (int i : occupiedYList) {
                    bucketVariance += Math.pow(yHistogram[i] - meanPerBucket, 2);
                }
                bucketVariance /= occupiedBuckets;
                double bucketCV = (meanPerBucket > 0)
                        ? Math.sqrt(bucketVariance) / meanPerBucket : 0;

                // Đo độ đều của khoảng cách giữa các bucket (line spacing)
                double gapCV = 0;
                if (occupiedYList.size() >= 4) {
                    List<Integer> gaps = new ArrayList<>();
                    for (int i = 1; i < occupiedYList.size(); i++) {
                        gaps.add(occupiedYList.get(i) - occupiedYList.get(i - 1));
                    }
                    double meanGap = gaps.stream().mapToInt(Integer::intValue).average().orElse(0);
                    double gapVariance = gaps.stream()
                            .mapToDouble(g -> Math.pow(g - meanGap, 2))
                            .average().orElse(0);
                    gapCV = (meanGap > 0) ? Math.sqrt(gapVariance) / meanGap : 0;
                }

                // Cả hai đều đặn → là printed text, dù ngắn hay dài
                boolean isUniformlyDistributed = bucketCV < 0.5 && gapCV < 0.4;
                if (occupiedBuckets > 6 && isUniformlyDistributed) return false;
                // Edge case: ít bucket hơn nhưng vẫn rất đều → cũng loại
                if (occupiedBuckets > 4 && bucketCV < 0.3 && gapCV < 0.3) return false;
            }

            // =====================================================
            // FILTER 2: Height CV — chữ in có chiều cao contour đồng đều
            // =====================================================
            List<Double> contourHeights = new ArrayList<>();
            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area < 5) continue;
                Rect r = Imgproc.boundingRect(contour);
                contourHeights.add((double) r.height);
            }

            if (contourHeights.size() > 10) {
                double meanH = contourHeights.stream()
                        .mapToDouble(Double::doubleValue).average().orElse(0);
                double variance = contourHeights.stream()
                        .mapToDouble(h -> Math.pow(h - meanH, 2))
                        .average().orElse(0);
                double heightCV = (meanH > 0) ? Math.sqrt(variance) / meanH : 0;

                // Printed text: CV thấp (chữ đều cỡ)
                // Chữ ký: CV cao (nét biến thiên nhiều)
                if (heightCV < 0.35) return false;
            }

            // =====================================================
            // FILTER 3: Small uniform contour ratio — ký tự in nhỏ đều
            // =====================================================
            long smallUniformContours = contours.stream()
                    .filter(c -> {
                        double area = Imgproc.contourArea(c);
                        return area > 5 && area < 300;
                    }).count();

            double smallRatio = smallUniformContours / (double) contours.size();
            if (contours.size() > 30 && smallRatio > 0.70) return false;

            // =====================================================
            // SCORING: Các feature đặc trưng của chữ ký
            // =====================================================
            double totalArea      = 0;
            double totalPerimeter = 0;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE,
                    maxX = 0,             maxY = 0;

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                totalArea += area;
                MatOfPoint2f c2f = new MatOfPoint2f(contour.toArray());
                totalPerimeter += Imgproc.arcLength(c2f, true);
                Rect r = Imgproc.boundingRect(contour);
                minX = Math.min(minX, r.x);
                minY = Math.min(minY, r.y);
                maxX = Math.max(maxX, r.x + r.width);
                maxY = Math.max(maxY, r.y + r.height);
            }

            // Feature 1: Ink ratio
            double inkRatio   = totalArea / imageArea;
            boolean inkRatioOk = inkRatio > 0.005 && inkRatio < 0.25;

            // Feature 2: Contour density
            double contourDensity = contours.size() / (imageArea / 10000.0);
            boolean densityOk     = contourDensity > 10 && contourDensity < 800;

            // Feature 3: Stroke complexity (perimeter² / area)
            double complexity   = (totalArea > 0) ? (totalPerimeter * totalPerimeter) / totalArea : 0;
            boolean complexityOk = complexity > 50;

            // Feature 4: Aspect ratio bounding box tổng thể
            double aspectRatio = (maxY - minY) > 0
                    ? (double) (maxX - minX) / (maxY - minY) : 0;
            boolean aspectOk   = aspectRatio > 1.0 && aspectRatio < 10.0;

            int score = 0;
            if (inkRatioOk)   score += 3;
            if (densityOk)    score += 2;
            if (complexityOk) score += 3;
            if (aspectOk)     score += 2;

            return score >= 6;

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
            Signature oldSignature = signatureRepository.findByHash(hash);
            System.out.println("oldSignature: " + oldSignature);

            if(oldSignature!= null){
                if(newSignature.getHash().equals(oldSignature.getHash())){
                    oldSignature.setActive(true);
                    signatureRepository.save(oldSignature);
                    return true;
                }
            }

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
