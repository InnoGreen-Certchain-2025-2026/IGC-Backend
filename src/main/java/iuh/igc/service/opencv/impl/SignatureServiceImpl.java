package iuh.igc.service.opencv.impl;

import iuh.igc.service.opencv.SignatureService;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SignatureServiceImpl implements SignatureService {

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

        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc file: " + e.getMessage());
        }
    }
}
