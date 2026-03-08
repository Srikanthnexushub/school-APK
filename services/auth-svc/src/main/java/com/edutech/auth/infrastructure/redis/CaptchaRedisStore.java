package com.edutech.auth.infrastructure.redis;

import com.edutech.auth.domain.port.out.CaptchaChallengeStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CaptchaRedisStore implements CaptchaChallengeStore {

    private static final String PREFIX = "captcha:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;

    public CaptchaRedisStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void save(String id, String answer) {
        redis.opsForValue().set(PREFIX + id, answer.toUpperCase(), TTL);
    }

    /** Atomically fetch and delete (single-use). Returns null if expired or not found. */
    @Override
    public String findAndDelete(String id) {
        String key = PREFIX + id;
        String answer = redis.opsForValue().get(key);
        if (answer != null) {
            redis.delete(key);
        }
        return answer;
    }
}
