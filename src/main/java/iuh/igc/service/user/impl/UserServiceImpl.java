package iuh.igc.service.user.impl;

import iuh.igc.advice.exception.S3UploadException;
import iuh.igc.config.s3.S3Service;
import iuh.igc.dto.request.user.UpdateProfileRequest;
import iuh.igc.dto.response.user.UserProfileResponse;
import iuh.igc.dto.response.user.UserSessionResponse;
import iuh.igc.entity.User;
import iuh.igc.service.user.CurrentUserProvider;
import iuh.igc.service.user.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Admin 2/13/2026
 *
 **/
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

    // Service
    S3Service s3Service;

    // Provider
    CurrentUserProvider currentUserProvider;

    // Constant
    static Long MAX_AVATAR_FILE_SIZE = 5 * 1024 * 1024L;

    @Override
    public UserSessionResponse getUserSession() {
        User user = currentUserProvider.get();

        return new UserSessionResponse(
                user.getId(),
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

    @Override
    @Transactional
    public void updateUserAvatar(MultipartFile file) {
        User user = currentUserProvider.get();

        String folderName = "users/" + user.getId() + "/avatar";
        String oldAvatarUrl = user.getAvatarUrl();

        if (file != null) {
            String newAvatarUrl = s3Service.uploadFile(file, folderName, false, MAX_AVATAR_FILE_SIZE);
            user.setAvatarUrl(newAvatarUrl);

            try {

                if (oldAvatarUrl != null && !oldAvatarUrl.isBlank())
                    s3Service.deleteFileByKey(oldAvatarUrl);

            } catch (Exception e) {

                try {
                    s3Service.deleteFileByKey(newAvatarUrl);
                } catch (Exception ignored) {
                }

                user.setAvatarUrl(oldAvatarUrl);

                throw new S3UploadException(
                        "Lỗi khi xóa ảnh cũ, đã hoàn tác cập nhật.",
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
        }
    }


}
