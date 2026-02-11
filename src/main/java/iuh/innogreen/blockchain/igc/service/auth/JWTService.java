package iuh.innogreen.blockchain.igc.service.auth;

import iuh.innogreen.blockchain.igc.entity.User;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Admin 2/11/2026
 *
 **/
public interface JWTService {
    String buildJwt(User user, Long expirationRate);

    Jwt decodeJwt(String token);
}
