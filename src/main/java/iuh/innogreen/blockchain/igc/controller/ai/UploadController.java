package iuh.innogreen.blockchain.igc.controller.ai;

import iuh.innogreen.blockchain.igc.dto.base.ApiResponse;
import iuh.innogreen.blockchain.igc.dto.base.FileResponse;
import iuh.innogreen.blockchain.igc.service.ai.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
@Slf4j
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class UploadController {
    private final AiService aiService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> extract (
            @RequestParam("file") MultipartFile file){

        try(InputStream inputStream = file.getInputStream()){
            FileResponse response = aiService.extract(inputStream,file);
            return ResponseEntity.ok(response);
        }catch (IOException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading image");
        }
    }



}
