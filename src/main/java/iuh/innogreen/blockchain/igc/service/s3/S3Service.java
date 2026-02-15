package iuh.innogreen.blockchain.igc.service.s3;

import iuh.innogreen.blockchain.igc.advice.exception.S3UploadException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

/**
 * Admin 2/15/2026
 *
 **/
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class S3Service {

    S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    @NonFinal
    String awsBucketName;

    @Value("${aws.s3.domain}")
    @NonFinal
    String domain;

    /**
     * =============================================
     * Upload file
     * =============================================
     **/
    public String uploadFile(
            MultipartFile file,
            String folderName,
            boolean getUrl,
            long maxFileSize
    ) {
        try {
            validateFile(file, maxFileSize);

            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String extension = "";
            int i = originalFilename.lastIndexOf('.');
            if (i > 0) {
                extension = originalFilename.substring(i);
            }

            String randomFileName = UUID.randomUUID().toString() + extension;

            String key = buildKey(folderName, randomFileName);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(awsBucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            try (InputStream inputStream = file.getInputStream()) {
                RequestBody requestBody = RequestBody.fromInputStream(inputStream, file.getSize());
                s3Client.putObject(putRequest, requestBody);
            }

            return getUrl ? domain + "/" + key : key;

        } catch (Exception e) {
            throw new S3UploadException("Không tải được dữ liệu tệp: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * =============================================
     * Delete file
     * =============================================
     **/
    public void deleteFileByKey(String key) {
        try {
            if (key == null || key.isBlank()) {
                throw new S3UploadException("Key không hợp lệ", HttpStatus.NOT_FOUND);
            }

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(awsBucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
        } catch (Exception e) {
            throw new S3UploadException("Lỗi khi xóa tệp trên S3", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * =============================================
     * Hàm phụ
     * =============================================
     **/

    private void validateFile(MultipartFile file, long maxFileSize) {
        if (file == null || file.isEmpty())
            throw new S3UploadException("Tệp gửi lên bị rỗng", HttpStatus.BAD_REQUEST);

        if (file.getSize() > maxFileSize)
            throw new S3UploadException("Tệp quá lớn (> " + maxFileSize + " bytes)", HttpStatus.CONTENT_TOO_LARGE);
    }

    private String buildKey(String folderName, String fileName) {

        if (folderName == null || folderName.isBlank()) {
            return fileName;
        }

        // Xóa dấu "/" ở cuối nếu có
        if (folderName.endsWith("/")) {
            folderName = folderName.substring(0, folderName.length() - 1);
        }

        // Xóa dấu "/" ở đầu nếu có
        if (folderName.startsWith("/")) {
            folderName = folderName.substring(1);
        }
        return folderName + "/" + fileName;

    }

}
