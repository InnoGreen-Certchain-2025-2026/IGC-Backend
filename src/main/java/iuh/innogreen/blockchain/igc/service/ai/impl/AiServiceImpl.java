package iuh.innogreen.blockchain.igc.service.ai.impl;

import iuh.innogreen.blockchain.igc.dto.base.FileResponse;
import iuh.innogreen.blockchain.igc.service.ai.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;


    @Override
    public FileResponse extract(InputStream inputStream,MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            log.info("Sending image to AI: {} ({} bytes)", file.getOriginalFilename(), bytes.length);

            var mimeType = MimeTypeUtils.parseMimeType(
                    file.getContentType() != null ? file.getContentType() : "image/jpeg"
            );

            return chatClient.prompt()
                    .user(u -> u
                            .text("""
                                    Please read the attached receipt and return the value in provided format
                                    """)
                            .media(
                                    MimeTypeUtils.parseMimeType(file.getContentType()),new InputStreamResource(inputStream)
                            )
                    )
                    .call()
                    .entity(FileResponse.class);

        } catch (Exception e) {
            log.error("AI extraction failed: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể trích xuất từ ảnh: " + e.getMessage(), e);
        }
    }
}