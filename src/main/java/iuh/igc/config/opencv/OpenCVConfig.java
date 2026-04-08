package iuh.igc.config.opencv;

import jakarta.annotation.PostConstruct;
import nu.pattern.OpenCV;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenCVConfig {
    @PostConstruct
    public void init() {
        OpenCV.loadShared();
    }
}
