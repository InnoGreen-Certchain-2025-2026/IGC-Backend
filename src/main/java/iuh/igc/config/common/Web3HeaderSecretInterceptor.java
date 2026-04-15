package iuh.igc.config.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class Web3HeaderSecretInterceptor implements HandlerInterceptor {

    private static final String HEADER_NAME_STANDARD = "X-Header-Secret";
    private static final String HEADER_NAME_LEGACY = "X_HEADER_SECRET";

    @Value("${security.web3.x-header-secret:}")
    private String expectedSecret;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        if (!StringUtils.hasText(expectedSecret)) {
            log.error("security.web3.x-header-secret is missing while Web3 header guard is active");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server security configuration is invalid");
            return false;
        }

        String actualSecret = request.getHeader(HEADER_NAME_STANDARD);
        if (!StringUtils.hasText(actualSecret)) {
            actualSecret = request.getHeader(HEADER_NAME_LEGACY);
        }
        if (!expectedSecret.equals(actualSecret)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid X-Header-Secret header");
            return false;
        }

        return true;
    }
}