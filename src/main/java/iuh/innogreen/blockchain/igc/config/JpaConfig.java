package iuh.innogreen.blockchain.igc.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Admin 2/9/2026
 *
 **/
@Configuration
public class JpaConfig {

    @Bean(name = "auditorProvider")
    public AuditorAware<@NonNull String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder
                        .getContext()
                        .getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .or(() -> Optional.of("SYSTEM"));
    }

}
