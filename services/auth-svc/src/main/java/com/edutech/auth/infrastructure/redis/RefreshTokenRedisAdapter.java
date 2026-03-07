// src/main/java/com/edutech/auth/infrastructure/redis/RefreshTokenRedisAdapter.java
package com.edutech.auth.infrastructure.redis;

import com.edutech.auth.application.dto.StoredRefreshToken;
import com.edutech.auth.domain.port.out.TokenStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Redis adapter for refresh token storage.
 *
 * Key schema:
 *   rt:{tokenId}         → JSON StoredRefreshToken (TTL = token expiry)
 *   rt:user:{userId}     → Redis Set of active tokenIds for that user (for logout-all)
 */
@Component
public class RefreshTokenRedisAdapter implements TokenStore {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenRedisAdapter.class);
    private static final String TOKEN_PREFIX = "rt:";
    private static final String USER_INDEX_PREFIX = "rt:user:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RefreshTokenRedisAdapter(StringRedisTemplate redisTemplate,
                                    ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(String tokenId, StoredRefreshToken token, long ttlSeconds) {
        String key = TOKEN_PREFIX + tokenId;
        String userIndexKey = USER_INDEX_PREFIX + token.userId();
        try {
            String json = objectMapper.writeValueAsString(token);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            redisTemplate.opsForSet().add(userIndexKey, tokenId);
            redisTemplate.expire(userIndexKey, Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize refresh token", e);
        }
    }

    @Override
    public Optional<StoredRefreshToken> find(String tokenId) {
        String json = redisTemplate.opsForValue().get(TOKEN_PREFIX + tokenId);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, StoredRefreshToken.class));
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize refresh token id={}", tokenId, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String tokenId) {
        String json = redisTemplate.opsForValue().get(TOKEN_PREFIX + tokenId);
        if (json != null) {
            try {
                StoredRefreshToken token = objectMapper.readValue(json, StoredRefreshToken.class);
                redisTemplate.opsForSet().remove(USER_INDEX_PREFIX + token.userId(), tokenId);
            } catch (JsonProcessingException e) {
                log.warn("Could not remove tokenId from user index during delete: {}", tokenId);
            }
        }
        redisTemplate.delete(TOKEN_PREFIX + tokenId);
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        String userIndexKey = USER_INDEX_PREFIX + userId;
        Set<String> tokenIds = redisTemplate.opsForSet().members(userIndexKey);
        if (tokenIds != null) {
            tokenIds.forEach(id -> redisTemplate.delete(TOKEN_PREFIX + id));
        }
        redisTemplate.delete(userIndexKey);
        log.info("Revoked all refresh tokens for userId={}", userId);
    }
}
