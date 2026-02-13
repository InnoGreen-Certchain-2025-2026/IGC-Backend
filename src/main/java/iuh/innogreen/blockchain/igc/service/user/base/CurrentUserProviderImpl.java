package iuh.innogreen.blockchain.igc.service.user.base;

import iuh.innogreen.blockchain.igc.entity.User;
import iuh.innogreen.blockchain.igc.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Admin 2/11/2026
 *
 **/
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CurrentUserProviderImpl implements iuh.innogreen.blockchain.igc.service.user.CurrentUserProvider {

    UserRepository userRepository;

    @Override
    public User get() {
        // Lấy email của người dùng hiện tại từ SecurityContext.
        // Đây là nơi Spring Security giữ thông tin đăng nhập sau khi xác thực.
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        // ===================================================================================================
        // THÔNG TIN BỔ SUNG
        //
        // Khi gửi request lên, client luôn phải kèm theo JWT trong header.
        // Bên trong JWT có chứa email và một số thông tin cơ bản khác.
        // Spring Security sẽ tự động tách JWT, kiểm tra và lưu thông tin vào SecurityContextHolder.
        // ===================================================================================================

        // Từ email đã lấy được, truy vấn xuống database để tìm user tương ứng.
        // Nếu không tìm thấy, ném ra ngoại lệ báo rằng không có người dùng nào khớp.

        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng hiện tại"));
    }

}
