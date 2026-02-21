package iuh.igc.service.ai;

import iuh.igc.dto.response.core.FileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface AiService {
    FileResponse extract(InputStream inputStream, MultipartFile file);
}
