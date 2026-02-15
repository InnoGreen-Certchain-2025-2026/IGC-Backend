package iuh.innogreen.blockchain.igc.controller;

import iuh.innogreen.blockchain.igc.dto.base.ApiResponse;
import iuh.innogreen.blockchain.igc.dto.response.core.FileResponse;
import iuh.innogreen.blockchain.igc.service.ai.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class UploadController {
    private final AiService aiService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileResponse>> extract(
            @RequestPart("file") MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            FileResponse response = aiService.extract(inputStream, file);
            return ResponseEntity.ok(new ApiResponse<>(response));
        } catch (IOException e) {
            log.error("Error processing file upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(null));
        }
    }
}
