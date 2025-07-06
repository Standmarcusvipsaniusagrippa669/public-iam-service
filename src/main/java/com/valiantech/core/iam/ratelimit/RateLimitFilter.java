package com.valiantech.core.iam.ratelimit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class RateLimitFilter implements Filter {

    private final Bucket4jRateLimiter rateLimiter;
    private final Set<String> whitelist;

    public RateLimitFilter(Bucket4jRateLimiter rateLimiter, @Value("${RATE_LIMIT_WHITELIST:}") String whitelistEnv) {
        this.rateLimiter = rateLimiter;
        this.whitelist = new HashSet<>(Arrays.asList(whitelistEnv.split(",")));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();
        String remoteIp = req.getRemoteAddr();
        String userId = extractUserIdFromJwt(req); // Implementa esto según tu filtro JWT

        // Excluye rutas y recursos estáticos
        if (path.startsWith("/api/v1/auth") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-resources") ||
                path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".ico") || path.endsWith(".png") || path.endsWith(".map")) {
            chain.doFilter(request, response);
            return;
        }

        // Normaliza IPs por si vienen con espacios/etc.
        boolean isWhite = whitelist.stream().map(String::trim).anyMatch(ip -> ip.equals(remoteIp));

        String key = Objects.isNull(userId) ? "ip:" + remoteIp : "user:" + userId;
        long capacity = isWhite ? 100 : 10; // 100 or 10 req/min
        long refill = isWhite ? 100 : 10;
        Duration period = Duration.ofMinutes(1);

        var bucket = rateLimiter.resolveBucket(key, capacity, refill, period);
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.setStatus(429);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Too Many Requests\"}");
        }
    }

    // Implementa según cómo extraes JWT y claims en tu app
    private String extractUserIdFromJwt(HttpServletRequest request) {
        // Por ejemplo, si usas un filtro JWT y guardas el user como principal
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.valiantech.core.iam.user.model.User user) {
            return user.getId().toString();
        }
        // O, si guardas el claim en details
        if (auth != null && auth.getDetails() instanceof java.util.Map details) {
            Object userId = details.get("userId");
            if (userId != null) return userId.toString();
        }
        return null;
    }

}
