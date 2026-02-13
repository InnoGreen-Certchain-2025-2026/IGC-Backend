package iuh.innogreen.blockchain.igc.service.user.impl;

import iuh.innogreen.blockchain.igc.dto.response.user.UserSessionResponse;
import iuh.innogreen.blockchain.igc.entity.User;
import iuh.innogreen.blockchain.igc.repository.UserRepository;
import iuh.innogreen.blockchain.igc.service.user.CurrentUserProvider;
import iuh.innogreen.blockchain.igc.service.user.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

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


}
