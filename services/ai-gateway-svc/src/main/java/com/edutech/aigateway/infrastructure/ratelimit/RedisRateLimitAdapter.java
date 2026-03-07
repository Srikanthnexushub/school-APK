package com.edutech.aigateway.infrastructure.ratelimit;

import com.edutech.aigateway.domain.model.ModelType;
import com.edutech.aigateway.domain.port.out.RateLimitPort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RedisRateLimitAdapter implements RateLimitPort {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    public RedisRateLimitAdapter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Boolean> checkAndIncrement(String requesterId, ModelType modelType) {
        String key = "ratelimit:" + requesterId + ":" + modelType.name();
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1L) {
                        // First request in this window — set TTL of 60 seconds
                        return redisTemplate.expire(key, Duration.ofSeconds(60))
                                .thenReturn(true);
                    }
                    return Mono.just(count <= MAX_REQUESTS_PER_MINUTE);
                })
                .onErrorReturn(true); // On Redis error, fail open — allow the request
    }
}
