package com.edutech.auth.application.service;

import com.edutech.auth.application.config.MfaProperties;
import com.edutech.auth.application.config.OtpProperties;
import com.edutech.auth.application.exception.InvalidResetTokenException;
import com.edutech.auth.application.exception.OtpExpiredException;
import com.edutech.auth.application.exception.OtpMaxAttemptsExceededException;
import com.edutech.auth.application.exception.UserNotFoundException;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.in.PasswordResetUseCase;
import com.edutech.auth.domain.port.out.NotificationSender;
import com.edutech.auth.domain.port.out.OtpStore;
import com.edutech.auth.domain.port.out.PasswordHasher;
import com.edutech.auth.domain.port.out.TokenStore;
import com.edutech.auth.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
public class PasswordResetService implements PasswordResetUseCase {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String RESET_TOKEN_PREFIX = "pwd-reset:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final OtpStore otpStore;
    private final NotificationSender notificationSender;
    private final PasswordHasher passwordHasher;
    private final TokenStore tokenStore;
    private final OtpProperties otpProperties;
    private final MfaProperties mfaProperties;
    private final StringRedisTemplate redisTemplate;

    public PasswordResetService(UserRepository userRepository,
                                OtpStore otpStore,
                                NotificationSender notificationSender,
                                PasswordHasher passwordHasher,
                                TokenStore tokenStore,
                                OtpProperties otpProperties,
                                MfaProperties mfaProperties,
                                StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.otpStore = otpStore;
        this.notificationSender = notificationSender;
        this.passwordHasher = passwordHasher;
        this.tokenStore = tokenStore;
        this.otpProperties = otpProperties;
        this.mfaProperties = mfaProperties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void sendPasswordResetOtp(String email) {
        // Look up user but do NOT reveal whether email exists (no user enumeration)
        if (!userRepository.existsByEmail(email)) {
            log.debug("Password reset requested for unknown email={} — silently ignored", email);
            return;
        }

        String key = otpKey(email);
        int totalSends = otpStore.getResends(key);
        if (totalSends > otpProperties.maxResends()) {
            // Silently return rather than throwing — prevents timing-based email enumeration
            log.warn("Password reset OTP resend limit reached for email={}", email);
            return;
        }

        otpStore.incrementResends(key, 3600);

        int bound = (int) Math.pow(10, otpProperties.length());
        String otp = String.format("%0" + otpProperties.length() + "d",
            SECURE_RANDOM.nextInt(bound));
        otpStore.save(key, otp, otpProperties.expirySeconds());

        notificationSender.sendOtpEmail(email, otp, "PASSWORD_RESET",
            otpProperties.expirySeconds() / 60);
        log.info("Password reset OTP sent to email={}", email);
    }

    @Override
    public String verifyPasswordResetOtp(String email, String otp) {
        String key = otpKey(email);

        int attempts = otpStore.getAttempts(key);
        if (attempts >= otpProperties.maxAttempts()) {
            throw new OtpMaxAttemptsExceededException();
        }

        String stored = otpStore.find(key)
            .orElseThrow(OtpExpiredException::new);

        if (!stored.equals(otp)) {
            otpStore.incrementAttempts(key);
            throw new OtpExpiredException();
        }

        // Consume the OTP — single-use
        otpStore.delete(key);

        // Issue a short-lived reset token stored in Redis
        String resetToken = UUID.randomUUID().toString();
        String resetKey = RESET_TOKEN_PREFIX + email + ":" + resetToken;
        redisTemplate.opsForValue().set(
            resetKey, "valid", Duration.ofSeconds(mfaProperties.resetTokenTtlSeconds()));

        log.info("Password reset OTP verified for email={} — reset token issued", email);
        return resetToken;
    }

    @Override
    @Transactional
    public void resetPassword(String email, String resetToken, String newPassword) {
        String resetKey = RESET_TOKEN_PREFIX + email + ":" + resetToken;
        String marker = redisTemplate.opsForValue().get(resetKey);
        if (marker == null) {
            throw new InvalidResetTokenException();
        }

        // Consume the reset token — single-use
        redisTemplate.delete(resetKey);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email));

        String newHash = passwordHasher.hash(newPassword);
        user.updatePassword(newHash);
        userRepository.save(user);

        // Invalidate all existing sessions — spec step 6
        tokenStore.deleteAllForUser(user.getId());
        log.info("Password reset completed for userId={} — all sessions revoked", user.getId());
    }

    private String otpKey(String email) {
        return "otp:" + email + ":PASSWORD_RESET";
    }
}
