package iuh.innogreen.blockchain.igc.service.auth.impl;

import iuh.innogreen.blockchain.igc.config.auth.AuthConfig;
import iuh.innogreen.blockchain.igc.entity.User;
import iuh.innogreen.blockchain.igc.service.auth.JWTService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Admin 2/11/2026
 *
 **/
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JWTServiceImpl implements JWTService {

    JwtEncoder jwtEncoder;
    JwtDecoder jwtDecoder;

    @Override
    public String buildJwt(User user, Long expirationRate) {
        // Lấy thời điểm hiện tại
        Instant now = Instant.now();

        // Tính toán thời điểm JWT sẽ hết hạn
        Instant validity = now.plus(expirationRate, ChronoUnit.SECONDS);

        // Khai báo phần Header của JWT
        // Ở đây chứa thông tin về thuật toán ký (MAC algorithm) mà hệ thống đang dùng
        JwsHeader jwsHeader = JwsHeader.with(AuthConfig.MAC_ALGORITHM).build();

        // Khai báo phần Body (Claims) của JWT, bao gồm:
        // + issuedAt: thời điểm token được tạo ra
        // + expiresAt: thời điểm token hết hạn
        // + subject: email của người dùng (được dùng làm định danh chính)
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(user.getEmail())
                .build();

        // Cuối cùng, encode JWT và lấy ra chuỗi token trả về
        return jwtEncoder
                .encode(JwtEncoderParameters.from(jwsHeader, claims))
                .getTokenValue();
    }

    @Override
    public Jwt decodeJwt(String token) {
        return jwtDecoder.decode(token);
    }

}
