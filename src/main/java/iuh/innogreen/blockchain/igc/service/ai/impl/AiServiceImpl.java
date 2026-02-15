package iuh.innogreen.blockchain.igc.service.ai.impl;

import iuh.innogreen.blockchain.igc.dto.response.core.FileResponse;
import iuh.innogreen.blockchain.igc.service.ai.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;


@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;
    @Override
    public FileResponse extract(InputStream inputStream, MultipartFile file) {
        return chatClient.prompt()
                .user(u->u
                        .text("Please read the attached event signup sheet image and extract all participants.")
                        .media(
                                MimeTypeUtils.parseMimeType(file.getContentType()),
                                new InputStreamResource(inputStream)
                        ))
                .call()
                .entity(FileResponse.class);
    }
}
