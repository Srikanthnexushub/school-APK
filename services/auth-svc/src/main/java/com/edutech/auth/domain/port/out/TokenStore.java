// src/main/java/com/edutech/auth/domain/port/out/TokenStore.java
package com.edutech.auth.domain.port.out;

import com.edutech.auth.application.dto.StoredRefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface TokenStore {
    void save(String tokenId, StoredRefreshToken token, long ttlSeconds);
    Optional<StoredRefreshToken> find(String tokenId);
    void delete(String tokenId);
    void deleteAllForUser(UUID userId);
}
