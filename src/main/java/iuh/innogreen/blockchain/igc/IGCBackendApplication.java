package iuh.innogreen.blockchain.igc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class IGCBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(IGCBackendApplication.class, args);
    }

}
