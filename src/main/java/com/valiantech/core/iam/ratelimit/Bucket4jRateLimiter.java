package com.valiantech.core.iam.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.serialization.Mapper;
import io.github.bucket4j.redis.jedis.Bucket4jJedis;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import org.springframework.stereotype.Component;
import redis.clients.jedis.UnifiedJedis;

import java.time.Duration;

import static java.time.Duration.ofSeconds;

/**
 * Servicio responsable de la gestión de rate limiting distribuido usando Bucket4j y Redis (Jedis).
 *
 * <p>
 * Esta clase permite obtener y configurar "buckets" (baldes de tokens) asociados a una clave arbitraria,
 * típicamente por IP, usuario o endpoint, para limitar la cantidad de solicitudes permitidas en un periodo de tiempo.
 * El estado de los buckets se almacena de forma distribuida en Redis mediante Jedis, lo que permite aplicar
 * límites consistentes incluso en aplicaciones escaladas horizontalmente.
 * </p>
 *
 * <p>
 * El bucket se configura con una estrategia de expiración basada en el tiempo necesario para recargarlo completamente,
 * lo cual optimiza el uso de memoria en Redis.
 * </p>
 *
 * <h3>Ejemplo de uso:</h3>
 * <pre>{@code
 * Bucket bucket = bucket4jRateLimiter.resolveBucket("login:ip:192.168.0.1", 5, 5, Duration.ofMinutes(1));
 * if (bucket.tryConsume(1)) {
 *     // Permitir acceso al endpoint
 * } else {
 *     // Retornar HTTP 429 Too Many Requests
 * }
 * }</pre>
 *
 * <p>
 * Es recomendable utilizar este servicio desde un aspecto AOP (para anotaciones como {@code @RateLimit})
 * o desde filtros HTTP, según la estrategia de protección de cada endpoint.
 * </p>
 *
 * <b>Notas:</b>
 * <ul>
 *     <li>La clave {@code key} define el ámbito del rate limit (por ejemplo: IP, usuario, endpoint).</li>
 *     <li>La política de refill es tipo <i>greedy</i>; puede ser ajustada según el tipo de endpoint.</li>
 *     <li>El tiempo de expiración en Redis es de 10 segundos después de recargarse completamente el bucket.</li>
 * </ul>
 *
 * @author Ian Cardenas
 * @since 1.0
 */
@Component
public class Bucket4jRateLimiter {
    private final JedisBasedProxyManager<String> proxyManager;

    /**
     * Inicializa el rate limiter usando Jedis y la estrategia de expiración recomendada para Bucket4j.
     *
     * @param unifiedJedis Cliente Jedis unificado (standalone o cluster).
     */
    public Bucket4jRateLimiter(UnifiedJedis unifiedJedis) {
        this.proxyManager = Bucket4jJedis.casBasedBuilder(unifiedJedis)
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(ofSeconds(10)))
                .keyMapper(Mapper.STRING)
                .build();
    }

    /**
     * Obtiene o crea un bucket para la clave dada, con la capacidad y refill especificados.
     *
     * @param key           Clave que identifica el bucket (por IP, usuario, endpoint, etc.).
     * @param capacity      Número máximo de tokens (requests) permitidos en el periodo.
     * @param refill        Cantidad de tokens a recargar en cada periodo.
     * @param refillDuration Duración del periodo de refill.
     * @return El bucket configurado para la clave.
     */
    public Bucket resolveBucket(String key, long capacity, long refill, Duration refillDuration) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(refill, refillDuration))
                .build();
        return proxyManager.getProxy(key, () -> configuration);
    }
}
