// src/main/java/com/edutech/auth/infrastructure/redis/OtpRedisAdapter.java
package com.edutech.auth.infrastructure.redis;

import com.edutech.auth.domain.port.out.OtpStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis adapter for OTP storage.
 *
 * Key schema:
 *   {key}         → OTP string (TTL = OTP expiry)
 *   {key}:att     → attempt counter (same TTL)
 *   {key}:rst     → resend counter (1-hour window)
 */
@Component
public class OtpRedisAdapter implements OtpStore {

    private static final String ATTEMPTS_SUFFIX = ":att";
    private static final String RST_SUFFIX = ":rst";

    private final StringRedisTemplate redisTemplate;

    public OtpRedisAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String key, String otp, int ttlSeconds) {
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        redisTemplate.opsForValue().set(key, otp, ttl);
        redisTemplate.opsForValue().set(key + ATTEMPTS_SUFFIX, "0", ttl);
    }

    @Override
    public Optional<String> find(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    @Override
    public void incrementAttempts(String key) {
        redisTemplate.opsForValue().increment(key + ATTEMPTS_SUFFIX);
    }

    @Override
    public int getAttempts(String key) {
        String val = redisTemplate.opsForValue().get(key + ATTEMPTS_SUFFIX);
        return val == null ? 0 : Integer.parseInt(val);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
        redisTemplate.delete(key + ATTEMPTS_SUFFIX);
    }

    @Override
    public int getResends(String key) {
        String val = redisTemplate.opsForValue().get(key + RST_SUFFIX);
        return val == null ? 0 : Integer.parseInt(val);
    }

    @Override
    public void incrementResends(String key, int ttlSeconds) {
        Long result = redisTemplate.opsForValue().increment(key + RST_SUFFIX);
        if (result != null && result == 1) {
            // First call — set TTL on the key
            redisTemplate.expire(key + RST_SUFFIX, Duration.ofSeconds(ttlSeconds));
        }
    }
}
