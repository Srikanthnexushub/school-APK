// src/main/java/com/edutech/auth/application/service/UserRegistrationService.java
package com.edutech.auth.application.service;

import com.edutech.auth.application.dto.RegisterChildRequest;
import com.edutech.auth.application.dto.RegisterChildResponse;
import com.edutech.auth.application.dto.RegisterRequest;
import com.edutech.auth.application.dto.TokenPair;
import com.edutech.auth.application.exception.CaptchaVerificationException;
import com.edutech.auth.application.exception.EmailAlreadyExistsException;
import com.edutech.auth.domain.event.UserRegisteredEvent;
import com.edutech.auth.domain.model.Role;
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

        // SUPER_ADMIN cannot self-register — only created by platform operators directly in DB.
        // INSTITUTION_ADMIN IS permitted to self-register (via the "Institution" flow in RegisterPage).
        if (request.role() == Role.SUPER_ADMIN) {
            throw new IllegalArgumentException("Self-registration is not permitted for this role");
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
            request.phoneNumber(),
            request.parentEmail()
        );

        User savedUser = userRepository.save(user);

        // Publish immutable audit event
        auditEventPublisher.publish(new UserRegisteredEvent(
            savedUser.getId(), savedUser.getEmail(),
            savedUser.getRole(), savedUser.getCenterId()
        ));

        // Send verification OTP asynchronously via notification topic
        otpService.sendOtp(savedUser.getEmail(), "EMAIL_VERIFICATION", "email");

        // Send parental consent request if student is under 13
        if (request.parentEmail() != null && !request.parentEmail().isBlank()) {
            try {
                otpService.sendOtp(request.parentEmail(), "PARENTAL_CONSENT", "email");
                log.info("Parental consent requested: userId={} parentEmail={}",
                         savedUser.getId(), request.parentEmail());
            } catch (Exception e) {
                log.warn("Parental consent email failed (non-fatal): userId={} parentEmail={} error={}",
                         savedUser.getId(), request.parentEmail(), e.getMessage());
            }
        }

        log.info("User registered: id={} email={} role={}", savedUser.getId(),
                 savedUser.getEmail(), savedUser.getRole());

        return tokenService.issueTokenPair(savedUser, request.deviceFingerprint());
    }

    /**
     * Registers a child account on behalf of an authenticated parent.
     * No captcha, no device fingerprint, no email OTP — the parent vouches for the account.
     * The child is created ACTIVE with emailVerified=true immediately.
     */
    @Transactional
    public RegisterChildResponse registerChild(RegisterChildRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        String passwordHash = passwordHasher.hash(request.password());

        User user = User.createActive(
            request.email(),
            passwordHash,
            Role.STUDENT,
            request.firstName(),
            request.lastName(),
            request.phoneNumber()
        );

        User saved = userRepository.save(user);
        log.info("Child account created by parent: id={} email={}", saved.getId(), saved.getEmail());

        return new RegisterChildResponse(saved.getId(), saved.getEmail());
    }
}
