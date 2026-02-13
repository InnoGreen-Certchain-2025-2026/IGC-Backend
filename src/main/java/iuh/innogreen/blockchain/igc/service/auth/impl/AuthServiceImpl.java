package iuh.innogreen.blockchain.igc.service.auth.impl;

import iuh.innogreen.blockchain.igc.dto.request.auth.LoginRequest;
import iuh.innogreen.blockchain.igc.dto.request.auth.RegisterRequest;
import iuh.innogreen.blockchain.igc.dto.response.user.UserSessionResponse;
import iuh.innogreen.blockchain.igc.entity.User;
import iuh.innogreen.blockchain.igc.repository.UserRepository;
import iuh.innogreen.blockchain.igc.service.auth.JWTService;
import iuh.innogreen.blockchain.igc.service.auth.model.AuthResultWrapper;
import iuh.innogreen.blockchain.igc.service.user.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin 2/13/2026
 *
 **/
@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements iuh.innogreen.blockchain.igc.service.auth.AuthService {

    AuthenticationManager authenticationManager;
    PasswordEncoder passwordEncoder;

    JWTService jwtService;

    UserRepository userRepository;

    CurrentUserProvider currentUserProvider;

    @Value("${security.jwt.access-token-expiration}")
    @NonFinal
    Long accessTokenExpiration;

    @Value("${security.jwt.refresh-token-expiration}")
    @NonFinal
    Long refreshTokenExpiration;

    @Value("${security.cookie.same-site}")
    @NonFinal
    String sameSite;

    @Value("${security.cookie.secure}")
    @NonFinal
    boolean secure;

    @Value("${security.cookie.refresh-token-name}")
    @NonFinal
    String REFRESH_TOKEN_COOKIE_NAME;

    @Override
    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email()))
            throw new DataIntegrityViolationException("Email đã tồn tại");

        User user = User
                .builder()
                .email(request.email().toLowerCase())
                .name(request.name())
                .hashedPassword(passwordEncoder.encode(request.password()))
                .address(request.address() != null && !request.address().isBlank() ? request.address() : null)
                .dob(request.dob() != null ? request.dob() : null)
                .build();

        userRepository.save(user);
    }

    @Override
    public AuthResultWrapper login(LoginRequest loginRequest) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(
                loginRequest.email().toLowerCase(),
                loginRequest.password()
        );

        var authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return buildAuthResultWrapper(currentUserProvider.get());
    }

    @Override
    public ResponseCookie logout(String refreshToken) {
        if (refreshToken != null) {
            String email = jwtService.decodeJwt(refreshToken).getSubject();
            var refreshTokenUser = userRepository
                    .findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));
        }

        return ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .sameSite(sameSite)
                .secure(secure)
                .maxAge(0)
                .build();
    }

    @Override
    public AuthResultWrapper refreshSession(
            String refreshToken
    ) {
        String email = jwtService.decodeJwt(refreshToken).getSubject();
        var refreshTokenUser = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        return buildAuthResultWrapper(refreshTokenUser);
    }

    // =====================================
    // Utilities methods
    // =====================================

    private AuthResultWrapper buildAuthResultWrapper(
            User user
    ) {
        // ================================================
        // CREATE TOKEN
        // ================================================
        var accessToken = jwtService.buildJwt(user, accessTokenExpiration);
        var refreshToken = jwtService.buildJwt(user, refreshTokenExpiration);


        var refreshTokenCookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .path("/")
                .sameSite(sameSite)
                .secure(secure)
                .maxAge(refreshTokenExpiration)
                .build();


        UserSessionResponse userSessionResponse = UserSessionResponse
                .builder()
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .build();

        return new AuthResultWrapper(
                userSessionResponse,
                accessToken,
                refreshTokenCookie
        );
    }


}
