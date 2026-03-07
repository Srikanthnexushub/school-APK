// src/main/java/com/edutech/auth/application/service/UserRegistrationService.java
package com.edutech.auth.application.service;

import com.edutech.auth.application.dto.RegisterRequest;
import com.edutech.auth.application.dto.TokenPair;
import com.edutech.auth.application.exception.CaptchaVerificationException;
import com.edutech.auth.application.exception.EmailAlreadyExistsException;
import com.edutech.auth.domain.event.UserRegisteredEvent;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.in.RegisterUserUseCase;
import com.edutech.auth.domain.port.out.AuditEventPublisher;
import com.edutech.auth.domain.port.out.CaptchaVerifier;
import com.edutech.auth.domain.port.out.PasswordHasher;
import com.edutech.auth.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService implements RegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationService.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final CaptchaVerifier captchaVerifier;
    private final TokenService tokenService;
    private final OtpService otpService;
    private final AuditEventPublisher auditEventPublisher;

    public UserRegistrationService(UserRepository userRepository,
                                   PasswordHasher passwordHasher,
                                   CaptchaVerifier captchaVerifier,
                                   TokenService tokenService,
                                   OtpService otpService,
                                   AuditEventPublisher auditEventPublisher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.captchaVerifier = captchaVerifier;
        this.tokenService = tokenService;
        this.otpService = otpService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Override
    @Transactional
    public TokenPair register(RegisterRequest request, String ipAddress, String userAgent) {
        if (!captchaVerifier.verify(request.captchaToken(), ipAddress)) {
            throw new CaptchaVerificationException("Captcha verification failed");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        String passwordHash = passwordHasher.hash(request.password());

        User user = User.create(
            request.email(),
            passwordHash,
            request.role(),
            request.centerId(),
            request.firstName(),
            request.lastName(),
            request.phoneNumber()
        );

        User savedUser = userRepository.save(user);

        // Publish immutable audit event
        auditEventPublisher.publish(new UserRegisteredEvent(
            savedUser.getId(), savedUser.getEmail(),
            savedUser.getRole(), savedUser.getCenterId()
        ));

        // Send verification OTP asynchronously via notification topic
        otpService.sendOtp(savedUser.getEmail(), "EMAIL_VERIFICATION", "email");

        log.info("User registered: id={} email={} role={}", savedUser.getId(),
                 savedUser.getEmail(), savedUser.getRole());

        return tokenService.issueTokenPair(savedUser, request.deviceFingerprint());
    }
}
