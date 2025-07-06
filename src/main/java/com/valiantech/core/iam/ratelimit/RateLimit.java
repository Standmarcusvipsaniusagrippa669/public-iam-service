package com.valiantech.core.iam.ratelimit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    long capacity();
    long refill();
    long refillDurationSeconds() default 60;
}