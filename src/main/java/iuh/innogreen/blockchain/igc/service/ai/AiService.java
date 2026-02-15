package iuh.innogreen.blockchain.igc.service.ai;

import iuh.innogreen.blockchain.igc.dto.response.core.FileResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface AiService {
    FileResponse extract(InputStream inputStream, MultipartFile file);
}
