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

@Component
public class Bucket4jRateLimiter {
    private final JedisBasedProxyManager<String> proxyManager;

    public Bucket4jRateLimiter(UnifiedJedis unifiedJedis) {
        this.proxyManager = Bucket4jJedis.casBasedBuilder(unifiedJedis)
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(ofSeconds(10)))
                .keyMapper(Mapper.STRING)
                .build();
    }

    public Bucket resolveBucket(String key, long capacity, long refill, Duration refillDuration) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(refill, refillDuration))
                .build();
        return proxyManager.getProxy(key, () -> configuration);
    }
}
