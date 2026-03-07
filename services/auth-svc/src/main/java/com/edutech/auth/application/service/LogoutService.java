// src/main/java/com/edutech/auth/application/service/LogoutService.java
package com.edutech.auth.application.service;

import com.edutech.auth.domain.event.UserLogoutEvent;
import com.edutech.auth.domain.port.in.LogoutUseCase;
import com.edutech.auth.domain.port.out.AuditEventPublisher;
import com.edutech.auth.domain.port.out.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LogoutService implements LogoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(LogoutService.class);

    private final TokenStore tokenStore;
    private final AuditEventPublisher auditEventPublisher;

    public LogoutService(TokenStore tokenStore, AuditEventPublisher auditEventPublisher) {
        this.tokenStore = tokenStore;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Override
    public void logout(String refreshTokenId, UUID userId) {
        tokenStore.delete(refreshTokenId);
        auditEventPublisher.publish(new UserLogoutEvent(userId, refreshTokenId, false));
        log.info("User logged out: userId={} tokenId={}", userId, refreshTokenId);
    }

    @Override
    public void logoutAllDevices(UUID userId) {
        tokenStore.deleteAllForUser(userId);
        auditEventPublisher.publish(new UserLogoutEvent(userId, null, true));
        log.info("All sessions revoked: userId={}", userId);
    }
}
