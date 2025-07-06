package com.valiantech.core.iam.ratelimit;

import java.lang.annotation.*;

/**
 * Anotación para declarar límites de solicitudes (rate limiting) en métodos de controllers o servicios.
 *
 * <p>
 * Permite especificar la capacidad máxima de solicitudes permitidas en un periodo de tiempo, así como la
 * política de recarga de tokens (refill) para controlar el acceso concurrente o proteger endpoints críticos.
 * Debe estar soportada por un aspecto AOP (por ejemplo, {@link com.valiantech.core.iam.ratelimit.RateLimitAspect})
 * que aplique la lógica de rate limiting usando Bucket4j.
 * </p>
 *
 * <h3>Ejemplo de uso:</h3>
 * <pre>
 * &#64;RateLimit(capacity = 5, refill = 5, refillDurationSeconds = 60)
 * public ResponseEntity&lt;?&gt; login(@RequestBody LoginRequest request) {
 *     // lógica de login
 * }
 * </pre>
 *
 * <b>Notas:</b>
 * <ul>
 *   <li><b>capacity:</b> cantidad máxima de solicitudes permitidas en el periodo de refill.</li>
 *   <li><b>refill:</b> número de tokens recargados cada periodo (refillDurationSeconds).</li>
 *   <li><b>refillDurationSeconds:</b> duración del periodo de recarga, en segundos (por defecto: 60s).</li>
 *   <li>El tipo de refill predeterminado es "greedy", pero puede adaptarse en la implementación.</li>
 *   <li>El rate limit puede aplicarse por IP, usuario o cualquier clave, según la lógica del aspecto.</li>
 * </ul>
 *
 * @see com.valiantech.core.iam.ratelimit.RateLimitAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    long capacity();
    long refill();
    long refillDurationSeconds() default 60;
}