package iuh.innogreen.blockchain.igc.service.user;

import iuh.innogreen.blockchain.igc.dto.request.user.UpdateProfileRequest;
import iuh.innogreen.blockchain.igc.dto.response.user.UserProfileResponse;
import iuh.innogreen.blockchain.igc.dto.response.user.UserSessionResponse;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin 2/13/2026
 *
 **/
public interface UserService {
    UserSessionResponse getUserSession();

    UserProfileResponse getUserProfile();

    @Transactional
    void updateUserProfile(UpdateProfileRequest updateProfileRequest);
}
