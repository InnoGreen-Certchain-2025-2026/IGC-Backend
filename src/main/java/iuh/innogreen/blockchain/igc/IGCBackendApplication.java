package iuh.innogreen.blockchain.igc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.servlet.context.ServletComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableMethodSecurity
@ServletComponentScan
public class IGCBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(IGCBackendApplication.class, args);
    }

}
