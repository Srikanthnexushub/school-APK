// src/main/java/com/edutech/auth/application/service/TokenRefreshService.java
package com.edutech.auth.application.service;

import com.edutech.auth.application.dto.DeviceFingerprint;
import com.edutech.auth.application.dto.TokenPair;
import com.edutech.auth.domain.event.TokenRefreshedEvent;
import com.edutech.auth.domain.port.in.RefreshTokenUseCase;
import com.edutech.auth.domain.port.out.AuditEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class TokenRefreshService implements RefreshTokenUseCase {

    private final TokenService tokenService;
    private final AuditEventPublisher auditEventPublisher;

    public TokenRefreshService(TokenService tokenService,
                               AuditEventPublisher auditEventPublisher) {
        this.tokenService = tokenService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Override
    public TokenPair refresh(String refreshTokenId, DeviceFingerprint deviceFingerprint) {
        TokenPair newPair = tokenService.rotateRefreshToken(refreshTokenId, deviceFingerprint);

        // Audit: old token ID rotated to new token ID
        auditEventPublisher.publish(new TokenRefreshedEvent(
            null, refreshTokenId, newPair.refreshToken()
        ));

        return newPair;
    }
}
