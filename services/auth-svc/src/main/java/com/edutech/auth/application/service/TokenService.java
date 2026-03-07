// src/main/java/com/edutech/auth/application/service/TokenService.java
package com.edutech.auth.application.service;

import com.edutech.auth.application.dto.DeviceFingerprint;
import com.edutech.auth.application.dto.StoredRefreshToken;
import com.edutech.auth.application.dto.TokenPair;
import com.edutech.auth.application.exception.InvalidTokenException;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.out.AccessTokenGenerator;
import com.edutech.auth.domain.port.out.TokenStore;
import com.edutech.auth.domain.port.out.UserRepository;
import com.edutech.auth.application.config.JwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service managing the full token lifecycle:
 * issue, rotate (single-use refresh), and revoke.
 * Not a use case — used by other use case services.
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final AccessTokenGenerator accessTokenGenerator;
    private final TokenStore tokenStore;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    public TokenService(AccessTokenGenerator accessTokenGenerator,
                        TokenStore tokenStore,
                        UserRepository userRepository,
                        JwtProperties jwtProperties) {
        this.accessTokenGenerator = accessTokenGenerator;
        this.tokenStore = tokenStore;
        this.userRepository = userRepository;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Issues a new access + refresh token pair for the given user and device.
     */
    public TokenPair issueTokenPair(User user, DeviceFingerprint deviceFingerprint) {
        String fingerprintHash = deviceFingerprint.toFingerprintHash();
        Instant now = Instant.now();
        Instant accessExpiry = now.plusSeconds(jwtProperties.accessTokenExpirySeconds());
        Instant refreshExpiry = now.plusSeconds(jwtProperties.refreshTokenExpirySeconds());

        String accessToken = accessTokenGenerator.generate(user, fingerprintHash);

        String refreshTokenId = UUID.randomUUID().toString();
        StoredRefreshToken stored = new StoredRefreshToken(
            user.getId(), fingerprintHash, now, refreshExpiry
        );

        tokenStore.save(refreshTokenId, stored, jwtProperties.refreshTokenExpirySeconds());

        log.debug("Issued token pair for user={} tokenId={}", user.getId(), refreshTokenId);
        return new TokenPair(accessToken, refreshTokenId, accessExpiry, refreshExpiry);
    }

    /**
     * Single-use refresh token rotation: validates the old token, issues a new pair,
     * then deletes the old token (prevents reuse).
     */
    public TokenPair rotateRefreshToken(String refreshTokenId, DeviceFingerprint deviceFingerprint) {
        StoredRefreshToken stored = tokenStore.find(refreshTokenId)
            .orElseThrow(() -> new InvalidTokenException("Refresh token not found or already used"));

        if (stored.isExpired()) {
            tokenStore.delete(refreshTokenId);
            throw new InvalidTokenException("Refresh token has expired");
        }

        String incomingFp = deviceFingerprint.toFingerprintHash();
        if (!stored.isSameDevice(incomingFp)) {
            // Possible token theft — revoke ALL tokens for this user
            log.warn("Device fingerprint mismatch for user={}. Revoking all sessions.", stored.userId());
            tokenStore.deleteAllForUser(stored.userId());
            throw new InvalidTokenException("Device fingerprint mismatch — all sessions revoked");
        }

        User user = userRepository.findById(stored.userId())
            .orElseThrow(() -> new InvalidTokenException("User no longer exists"));

        // Delete old token before issuing new one
        tokenStore.delete(refreshTokenId);

        return issueTokenPair(user, deviceFingerprint);
    }

    public void revokeToken(String refreshTokenId) {
        tokenStore.delete(refreshTokenId);
    }

    public void revokeAllUserTokens(UUID userId) {
        tokenStore.deleteAllForUser(userId);
    }
}
