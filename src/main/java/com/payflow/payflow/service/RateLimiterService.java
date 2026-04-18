package com.payflow.payflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${rate.limit.requests}")
    private int maxRequests;

    @Value("${rate.limit.window}")
    private int windowSeconds;

    // Single method — does everything atomically and returns full state
    public RateLimitResult checkAndIncrement(String identifier) {
        String key = "rate_limit:" + identifier;

        String currentValue = redisTemplate.opsForValue().get(key);
        int currentCount = currentValue != null ? Integer.parseInt(currentValue) : 0;

        // Already over limit — don't increment, just return blocked state
        if (currentCount >= maxRequests) {
            Long ttl = redisTemplate.getExpire(key);
            long resetIn = (ttl != null && ttl > 0) ? ttl : windowSeconds;
            return new RateLimitResult(false, 0, resetIn);
        }

        // First request — set key with TTL window
        if (currentCount == 0) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(windowSeconds));
            return new RateLimitResult(true, maxRequests - 1, (long) windowSeconds);
        }

        // Increment existing key
        redisTemplate.opsForValue().increment(key);
        Long ttl = redisTemplate.getExpire(key);
        long resetIn = (ttl != null && ttl > 0) ? ttl : windowSeconds;
        int remaining = maxRequests - (currentCount + 1);

        return new RateLimitResult(true, remaining, resetIn);
    }

    // Simple result container
    public static class RateLimitResult {
        public final boolean allowed;
        public final int remaining;
        public final long resetSeconds;

        public RateLimitResult(boolean allowed, int remaining, long resetSeconds) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.resetSeconds = resetSeconds;
        }
    }
}