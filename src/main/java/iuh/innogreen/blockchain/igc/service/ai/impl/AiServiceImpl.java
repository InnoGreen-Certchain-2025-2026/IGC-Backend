package iuh.innogreen.blockchain.igc.service.ai.impl;

import iuh.innogreen.blockchain.igc.dto.base.FileResponse;
import iuh.innogreen.blockchain.igc.service.ai.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;

    private static final String PROMPT = """
            Bạn là hệ thống OCR chuyên đọc văn bằng và chứng chỉ sinh viên.

            Nhiệm vụ: Trích xuất thông tin từ ảnh và trả về MỘT JSON object hợp lệ duy nhất.
            KHÔNG thêm markdown, KHÔNG thêm giải thích — chỉ JSON thuần.

            Cấu trúc JSON bắt buộc (đúng thứ tự, đúng key):
            {
              "student_name":     "Họ và tên sinh viên hoặc null",
              "date_of_birth":    "YYYY-MM-DD hoặc null",
              "student_id":       "Mã số sinh viên hoặc CCCD hoặc null",
              "certificate_type": "Loại văn bằng (VD: Bằng Cử nhân, Chứng chỉ IELTS...) hoặc null",
              "major":            "Chuyên ngành hoặc null",
              "issue_date":       "YYYY-MM-DD hoặc null",
              "certificate_id":   "Số hiệu văn bằng / mã chứng chỉ hoặc null",
              "issuer":           "Tên trường hoặc tổ chức cấp hoặc null"
            }

            Quy tắc:
            - Ngày tháng PHẢI theo định dạng YYYY-MM-DD (VD: 2001-05-20).
            - Field không tìm thấy → null (không được bỏ key).
            - Giữ nguyên ngôn ngữ gốc (tiếng Việt hoặc tiếng Anh).
            """;

    @Override
    public FileResponse extract(MultipartFile file) {
        try {
            log.info(">>> File name: {}", file.getOriginalFilename());
            log.info(">>> Content type: {}", file.getContentType());
            log.info(">>> File size: {}", file.getSize());

            byte[] bytes = file.getBytes();
            log.info(">>> Bytes read: {}", bytes.length);

            var mimeType = MimeTypeUtils.parseMimeType(
                    file.getContentType() != null ? file.getContentType() : "image/jpeg"
            );

            var resource = new ByteArrayResource(bytes);

            log.info(">>> Calling OpenAI...");
            FileResponse result = chatClient.prompt()
                    .system(PROMPT)
                    .user(u -> u.media(mimeType, resource))
                    .call()
                    .entity(FileResponse.class);

            log.info(">>> Result: {}", result);
            return result;

        } catch (Exception e) {
            log.error(">>> EXCEPTION: {}", e.getMessage(), e);  // ← dòng quan trọng nhất
            throw new RuntimeException("Không thể trích xuất từ ảnh: " + e.getMessage(), e);
        }
    }
}