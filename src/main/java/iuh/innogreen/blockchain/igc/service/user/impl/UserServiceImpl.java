package iuh.innogreen.blockchain.igc.service.user.impl;

import iuh.innogreen.blockchain.igc.dto.request.user.UpdateProfileRequest;
import iuh.innogreen.blockchain.igc.dto.response.user.UserProfileResponse;
import iuh.innogreen.blockchain.igc.dto.response.user.UserSessionResponse;
import iuh.innogreen.blockchain.igc.entity.User;
import iuh.innogreen.blockchain.igc.repository.UserRepository;
import iuh.innogreen.blockchain.igc.service.user.CurrentUserProvider;
import iuh.innogreen.blockchain.igc.service.user.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin 2/13/2026
 *
 **/
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    CurrentUserProvider currentUserProvider;

    @Override
    public UserSessionResponse getUserSession() {
        User user = currentUserProvider.get();

        return new UserSessionResponse(
                user.getEmail(),
                user.getName(),
                user.getAvatarUrl()
        );
    }

    @Override
    public UserProfileResponse getUserProfile() {
        User user = currentUserProvider.get();

        return new UserProfileResponse(
                user.getName(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getDob(),
                user.getGender()
        );
    }

    @Transactional
    @Override
    public void updateUserProfile(UpdateProfileRequest updateProfileRequest) {
        User user = currentUserProvider.get();

        user.setName(updateProfileRequest.name());
        user.setPhoneNumber(updateProfileRequest.phoneNumber());
        user.setAddress(updateProfileRequest.address());
        user.setDob(updateProfileRequest.dob());
        user.setGender(updateProfileRequest.gender());
    }


}
