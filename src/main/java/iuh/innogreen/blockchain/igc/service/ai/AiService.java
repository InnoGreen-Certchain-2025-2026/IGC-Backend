package iuh.innogreen.blockchain.igc.service.ai;

import iuh.innogreen.blockchain.igc.dto.base.FileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface AiService {

    FileResponse extract(MultipartFile file);
}
