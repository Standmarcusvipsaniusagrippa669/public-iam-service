package com.valiantech.core.iam.ratelimit;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final Bucket4jRateLimiter rateLimiter;

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
