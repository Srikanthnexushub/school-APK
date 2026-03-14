// src/main/java/com/edutech/auth/application/service/OtpService.java
package com.edutech.auth.application.service;

import com.edutech.auth.application.dto.DeviceFingerprint;
import com.edutech.auth.application.dto.OtpSendResponse;
import com.edutech.auth.application.dto.TokenPair;
import com.edutech.auth.application.dto.OtpVerifyRequest;
import com.edutech.auth.application.exception.OtpExpiredException;
import com.edutech.auth.application.exception.OtpMaxAttemptsExceededException;
import com.edutech.auth.application.exception.OtpMaxResendsExceededException;
import com.edutech.auth.application.exception.UserNotFoundException;
import com.edutech.auth.domain.event.OtpRequestedEvent;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.in.VerifyOtpUseCase;
import com.edutech.auth.domain.port.out.AuditEventPublisher;
import com.edutech.auth.domain.port.out.NotificationSender;
import com.edutech.auth.domain.port.out.OtpStore;
import com.edutech.auth.domain.port.out.UserRepository;
import com.edutech.auth.application.config.OtpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class OtpService implements VerifyOtpUseCase {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OtpStore otpStore;
    private final NotificationSender notificationSender;
    private final UserRepository userRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final OtpProperties otpProperties;
    private final TokenService tokenService;

    public OtpService(OtpStore otpStore,
                      NotificationSender notificationSender,
                      UserRepository userRepository,
                      AuditEventPublisher auditEventPublisher,
                      OtpProperties otpProperties,
                      TokenService tokenService) {
        this.otpStore = otpStore;
        this.notificationSender = notificationSender;
        this.userRepository = userRepository;
        this.auditEventPublisher = auditEventPublisher;
        this.otpProperties = otpProperties;
        this.tokenService = tokenService;
    }

    @Override
    public OtpSendResponse sendOtp(String email, String purpose, String channel) {
        String key = buildKey(email, purpose);

        int totalSends = otpStore.getResends(key);
        if (totalSends > otpProperties.maxResends()) {
            throw new OtpMaxResendsExceededException();
        }

        otpStore.incrementResends(key, 3600);

        String otp = generateOtp(otpProperties.length());
        otpStore.save(key, otp, otpProperties.expirySeconds());

        int expiryMinutes = otpProperties.expirySeconds() / 60;
        if ("sms".equalsIgnoreCase(channel)) {
            notificationSender.sendOtpSms(email, otp, purpose, expiryMinutes);
        } else {
            notificationSender.sendOtpEmail(email, otp, purpose, expiryMinutes);
        }

        auditEventPublisher.publish(new OtpRequestedEvent(email, purpose, channel));
        log.debug("OTP sent: email={} purpose={} channel={}", email, purpose, channel);

        return new OtpSendResponse(
            Math.max(0, otpProperties.maxResends() - totalSends),
            otpProperties.maxResends()
        );
    }

    @Override
    @Transactional
    public java.util.Optional<TokenPair> verifyOtp(OtpVerifyRequest request) {
        String key = buildKey(request.email(), request.purpose());

        int attempts = otpStore.getAttempts(key);
        if (attempts >= otpProperties.maxAttempts()) {
            throw new OtpMaxAttemptsExceededException();
        }

        String storedOtp = otpStore.find(key)
            .orElseThrow(OtpExpiredException::new);

        if (!storedOtp.equals(request.otp())) {
            otpStore.incrementAttempts(key);
            throw new OtpExpiredException();
        }

        // Consumed — delete immediately (single-use)
        otpStore.delete(key);

        // If this was an email verification OTP, activate the user and issue a JWT
        if ("EMAIL_VERIFICATION".equals(request.purpose())) {
            User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException(request.email()));
            user.activate();
            userRepository.save(user);
            log.info("Account activated: email={}", request.email());
            DeviceFingerprint fp = new DeviceFingerprint("registration-verify", null, null);
            return Optional.of(tokenService.issueTokenPair(user, fp));
        }
        return Optional.empty();
    }

    private String generateOtp(int length) {
        int bound = (int) Math.pow(10, length);
        String raw = String.format("%0" + length + "d", SECURE_RANDOM.nextInt(bound));
        return raw;
    }

    private String buildKey(String email, String purpose) {
        return "otp:" + email + ":" + purpose;
    }
}
