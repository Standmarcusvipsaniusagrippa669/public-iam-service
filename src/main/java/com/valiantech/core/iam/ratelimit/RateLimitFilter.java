package com.valiantech.core.iam.ratelimit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Filtro global de rate limiting para la API.
 *
 * <p>
 * Este filtro se encarga de limitar la cantidad de solicitudes que puede realizar una misma IP o usuario autenticado
 * en un periodo de tiempo determinado, usando Bucket4j y Redis como backend distribuido.
 * Permite establecer una lista blanca de IPs (con mayor capacidad) mediante la variable de entorno {@code RATE_LIMIT_WHITELIST}.
 * </p>
 *
 * <ul>
 *   <li>Las rutas de Swagger, recursos estáticos y endpoints de monitoreo se excluyen del rate limiting.</li>
 *   <li>La clave de limitación puede ser la IP remota (para usuarios anónimos) o el ID de usuario extraído del JWT.</li>
 *   <li>La lista blanca se provee por variable de entorno separada por comas y permite capacidades mayores.</li>
 *   <li>Devuelve HTTP 429 Too Many Requests en caso de superar el límite configurado.</li>
 * </ul>
 *
 * <h3>Configuración recomendada:</h3>
 * <ul>
 *   <li><b>RATE_LIMIT_WHITELIST:</b> lista de IPs separadas por coma que estarán exentas o tendrán un límite más alto.</li>
 *   <li>Los límites pueden ajustarse según el endpoint y el ambiente.</li>
 * </ul>
 *
 * <h3>Ejemplo de integración:</h3>
 * <pre>
 * services:
 *   app:
 *     environment:
 *       - RATE_LIMIT_WHITELIST=192.168.1.1,10.0.0.5
 * </pre>
 *
 * <h3>Notas:</h3>
 * <ul>
 *   <li>El método {@link #extractUserIdFromJwt(HttpServletRequest)} debe adaptarse a cómo gestionas el usuario autenticado y los claims JWT.</li>
 *   <li>Para lógica más granular por endpoint, puedes combinar este filtro con la anotación {@code @RateLimit} y su aspecto.</li>
 * </ul>
 *
 * @author Ian Cardenas
 * @since 1.0
 */
@Component
public class RateLimitFilter implements Filter {

    private final Bucket4jRateLimiter rateLimiter;
    private final Set<String> whitelist;

    /**
     * Inicializa el filtro de rate limiting.
     *
     * @param rateLimiter Servicio de rate limiting basado en Bucket4j.
     * @param whitelistEnv Variable de entorno que contiene las IPs en lista blanca separadas por coma.
     */
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

        // Excluye rutas y recursos estáticos (Swagger, monitoreo, assets)
        if (path.contains("/actuator") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-resources") || path.endsWith(".css") || path.endsWith(".js") ||
                path.endsWith(".ico") || path.endsWith(".png") || path.endsWith(".map")) {
            chain.doFilter(request, response);
            return;
        }

        // Normaliza IPs por si vienen con espacios
        boolean isWhite = whitelist.stream().map(String::trim).anyMatch(ip -> ip.equals(remoteIp));

        String key = (userId == null) ? "ip:" + remoteIp : "user:" + userId;
        long capacity = isWhite ? 1500 : 1000;
        long refill = isWhite ? 1500 : 1000;
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

    /**
     * Extrae el ID de usuario autenticado desde el JWT o el contexto de seguridad.
     * Debe implementarse según la lógica específica del proyecto.
     *
     * @param request La solicitud HTTP actual.
     * @return El ID de usuario si está autenticado, o {@code null} si es anónimo.
     */
    private String extractUserIdFromJwt(HttpServletRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.valiantech.core.iam.user.model.User user) {
            return user.getId().toString();
        }
        if (auth != null && auth.getDetails() instanceof java.util.Map details) {
            Object userId = details.get("userId");
            if (userId != null) return userId.toString();
        }
        return null;
    }
}
