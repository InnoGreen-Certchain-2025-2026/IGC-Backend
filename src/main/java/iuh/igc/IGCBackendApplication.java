package iuh.igc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableMethodSecurity
public class IGCBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(IGCBackendApplication.class, args);
    }

}
