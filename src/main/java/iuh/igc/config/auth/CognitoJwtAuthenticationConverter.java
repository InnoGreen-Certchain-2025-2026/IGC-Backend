package iuh.igc.config.auth;

import iuh.igc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

/**
 * Converts a validated JWT into a Spring Security authentication token.
 *
 * Handles both:
 *  - Local JWTs (subject = email, issued by this backend)
 *  - Cognito JWTs (subject = UUID sub)
 *
 * No lazy sync is performed here — sync is handled by the /auth/sync endpoint.
 */
@Component
@RequiredArgsConstructor
public class CognitoJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String subject = jwt.getSubject();

        // Local JWT: subject is email. Cognito JWT: subject is UUID sub.
        // Try email lookup first, then cognitoSub lookup.
        boolean found = userRepository.findByEmail(subject.toLowerCase()).isPresent()
                     || userRepository.findByCognitoSub(subject).isPresent();

        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(found ? "ROLE_USER" : "ROLE_ANONYMOUS")
        );

        return new JwtAuthenticationToken(jwt, authorities, subject);
    }
}
