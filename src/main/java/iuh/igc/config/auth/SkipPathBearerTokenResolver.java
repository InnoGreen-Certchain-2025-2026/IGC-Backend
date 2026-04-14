package iuh.igc.config.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * Admin 2/11/2026
 *
 **/
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SkipPathBearerTokenResolver implements BearerTokenResolver {

    BearerTokenResolver delegate = new DefaultBearerTokenResolver();
    AntPathMatcher pathMatcher = new AntPathMatcher();
    static final List<SkipRule> SKIP_RULES = List.of(
            new SkipRule("/auth/logout", null),
            new SkipRule("/auth/register", null),
            new SkipRule("/auth/sync", null),
            new SkipRule("/api/certificates/verify/file", "POST"),
            new SkipRule("/api/certificates/*/verify", "GET"),
            new SkipRule("/api/certificates/claim/**", "GET")
    );

    @Override
    public String resolve(HttpServletRequest request) {
        String path = normalizePath(request.getRequestURI());
        String method = request.getMethod();

        for (SkipRule rule : SKIP_RULES) {
            if (rule.matches(pathMatcher, path, method)) {
                return null;
            }
        }

        return delegate.resolve(request);
    }

    private String normalizePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "/";
        }

        if (rawPath.length() > 1 && rawPath.endsWith("/")) {
            return rawPath.substring(0, rawPath.length() - 1);
        }

        return rawPath;
    }

    private record SkipRule(String pattern, String method) {
        boolean matches(AntPathMatcher pathMatcher, String requestPath, String requestMethod) {
            boolean pathMatched = pathMatcher.match(pattern, requestPath);
            if (!pathMatched) {
                return false;
            }

            return method == null || method.equalsIgnoreCase(requestMethod);
        }
    }
}
