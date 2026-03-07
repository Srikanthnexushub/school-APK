// src/main/java/com/edutech/auth/application/service/AuthenticationService.java
package com.edutech.auth.application.service;

import com.edutech.auth.application.dto.LoginRequest;
import com.edutech.auth.application.dto.TokenPair;
import com.edutech.auth.application.exception.AccountLockedException;
import com.edutech.auth.application.exception.AccountNotVerifiedException;
import com.edutech.auth.application.exception.CaptchaVerificationException;
import com.edutech.auth.application.exception.InvalidCredentialsException;
import com.edutech.auth.domain.event.UserAuthenticatedEvent;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.in.AuthenticateUserUseCase;
import com.edutech.auth.domain.port.out.AuditEventPublisher;
import com.edutech.auth.domain.port.out.CaptchaVerifier;
import com.edutech.auth.domain.port.out.PasswordHasher;
import com.edutech.auth.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService implements AuthenticateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final CaptchaVerifier captchaVerifier;
    private final TokenService tokenService;
    private final AuditEventPublisher auditEventPublisher;

    public AuthenticationService(UserRepository userRepository,
                                 PasswordHasher passwordHasher,
                                 CaptchaVerifier captchaVerifier,
                                 TokenService tokenService,
                                 AuditEventPublisher auditEventPublisher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.captchaVerifier = captchaVerifier;
        this.tokenService = tokenService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public TokenPair authenticate(LoginRequest request, String ipAddress, String userAgent) {
        if (!captchaVerifier.verify(request.captchaToken(), ipAddress)) {
            throw new CaptchaVerificationException("Captcha verification failed");
        }

        User user = userRepository.findByEmail(request.email())
            .orElse(null);

        if (user == null || !passwordHasher.verify(request.password(), user.getPasswordHash())) {
            // Publish failed auth event — do NOT reveal which check failed
            auditEventPublisher.publish(new UserAuthenticatedEvent(
                request.email(), ipAddress, userAgent, "INVALID_CREDENTIALS"
            ));
            log.warn("Failed login attempt for email={} ip={}", request.email(), ipAddress);
            throw new InvalidCredentialsException();
        }

        if (user.isLocked()) {
            auditEventPublisher.publish(new UserAuthenticatedEvent(
                request.email(), ipAddress, userAgent, "ACCOUNT_LOCKED"
            ));
            throw new AccountLockedException();
        }

        if (user.isPendingVerification()) {
            throw new AccountNotVerifiedException();
        }

        auditEventPublisher.publish(new UserAuthenticatedEvent(
            user.getId(), user.getEmail(), ipAddress, userAgent
        ));

        log.info("Successful login: userId={} ip={}", user.getId(), ipAddress);
        return tokenService.issueTokenPair(user, request.deviceFingerprint());
    }
}
