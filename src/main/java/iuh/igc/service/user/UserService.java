package iuh.igc.service.user;

import iuh.igc.dto.request.user.UpdateProfileRequest;
import iuh.igc.dto.response.user.UserProfileResponse;
import iuh.igc.dto.response.user.UserSessionResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin 2/13/2026
 *
 **/
public interface UserService {
    UserSessionResponse getUserSession();

    UserProfileResponse getUserProfile();

    @Transactional
    void updateUserProfile(UpdateProfileRequest updateProfileRequest);

    void updateUserAvatar(MultipartFile file);
}
