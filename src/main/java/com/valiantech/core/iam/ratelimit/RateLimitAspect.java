package com.valiantech.core.iam.ratelimit;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspecto AOP que implementa la lógica de rate limiting para los métodos anotados con {@link RateLimit}.
 *
 * <p>
 * Este aspecto intercepta la ejecución de los métodos que llevan la anotación {@code @RateLimit} y aplica
 * la política de limitación de solicitudes configurada en la anotación, utilizando el servicio {@link Bucket4jRateLimiter}
 * y la librería Bucket4j respaldada por Redis.
 * </p>
 *
 * <p>
 * Por defecto, el límite se aplica por dirección IP y endpoint (firma del método), aunque la clave puede ser ajustada
 * para aplicar otras políticas (por usuario, header, etc.). Si el bucket tiene tokens disponibles, la solicitud se ejecuta;
 * de lo contrario, se devuelve una respuesta HTTP 429 (Too Many Requests).
 * </p>
 *
 * <h3>Notas de uso:</h3>
 * <ul>
 *   <li>La clave utilizada es: {@code [nombreMetodo]:[ip]}.</li>
 *   <li>El consumo de tokens y la política de refill dependen de los parámetros de la anotación {@link RateLimit}.</li>
 *   <li>Si se desea personalizar el manejo de la excepción (por ejemplo, lanzar una excepción para un handler global),
 *       puede modificarse el bloque correspondiente.</li>
 *   <li>El aspecto puede adaptarse para usar otras claves según las necesidades del negocio (por usuario autenticado, por header, etc.).</li>
 * </ul>
 *
 * <h3>Ejemplo de integración:</h3>
 * <pre>
 * &#64;RateLimit(capacity = 5, refill = 5, refillDurationSeconds = 60)
 * public ResponseEntity&lt;?&gt; login(...) { ... }
 * </pre>
 *
 * @see RateLimit
 * @see Bucket4jRateLimiter
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final Bucket4jRateLimiter rateLimiter;

    /**
     * Intercepta los métodos anotados con {@link RateLimit} y aplica la lógica de rate limiting.
     *
     * @param joinPoint  El punto de ejecución interceptado.
     * @param rateLimit  La configuración de rate limit declarada en la anotación.
     * @return           El resultado del método original si no se excede el límite; de lo contrario, una respuesta HTTP 429.
     * @throws Throwable Propaga cualquier excepción lanzada por el método original.
     */
    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String ip = request.getRemoteAddr();
        String key = joinPoint.getSignature().toShortString() + ":" + ip;

        Bucket bucket = rateLimiter.resolveBucket(
                key,
                rateLimit.capacity(),
                rateLimit.refill(),
                java.time.Duration.ofSeconds(rateLimit.refillDurationSeconds())
        );

        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        } else {
            // Puedes lanzar una excepción custom si quieres usar tu handler global
            return org.springframework.http.ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"Too Many Requests\"}");
        }
    }
}
