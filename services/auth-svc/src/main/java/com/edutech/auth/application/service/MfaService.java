package com.edutech.auth.application.service;

import com.edutech.auth.application.config.MfaProperties;
import com.edutech.auth.application.dto.DeviceFingerprint;
import com.edutech.auth.application.dto.MfaSetupResponse;
import com.edutech.auth.application.dto.TokenPair;
import com.edutech.auth.application.exception.InvalidMfaCodeException;
import com.edutech.auth.application.exception.InvalidPendingMfaTokenException;
import com.edutech.auth.application.exception.MfaAlreadyEnabledException;
import com.edutech.auth.application.exception.MfaNotEnabledException;
import com.edutech.auth.application.exception.UserNotFoundException;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.in.MfaUseCase;
import com.edutech.auth.domain.port.out.UserRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Service
public class MfaService implements MfaUseCase {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
    private static final String PENDING_MFA_PREFIX = "pending-mfa:";
    // Temporary setup secret stored in Redis until the user confirms with their first TOTP code
    private static final String SETUP_SECRET_PREFIX = "mfa-setup:";

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final MfaProperties mfaProperties;
    private final StringRedisTemplate redisTemplate;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
    private final DefaultCodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

    public MfaService(UserRepository userRepository,
                      TokenService tokenService,
                      MfaProperties mfaProperties,
                      StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.mfaProperties = mfaProperties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public MfaSetupResponse initSetup(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (user.isMfaEnabled()) {
            throw new MfaAlreadyEnabledException();
        }

        String secret = secretGenerator.generate();

        // Store the unconfirmed secret temporarily — confirmed in confirmSetup()
        String setupKey = SETUP_SECRET_PREFIX + userId;
        redisTemplate.opsForValue().set(setupKey, secret, Duration.ofMinutes(10));

        String qrCodeUri = buildOtpAuthUri(user.getEmail(), secret);
        log.debug("MFA setup initiated for userId={}", userId);

        return new MfaSetupResponse(secret, qrCodeUri);
    }

    @Override
    @Transactional
    public void confirmSetup(UUID userId, String totpCode) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (user.isMfaEnabled()) {
            throw new MfaAlreadyEnabledException();
        }

        String setupKey = SETUP_SECRET_PREFIX + userId;
        String pendingSecret = redisTemplate.opsForValue().get(setupKey);
        if (pendingSecret == null) {
            throw new InvalidMfaCodeException();
        }

        if (!codeVerifier.isValidCode(pendingSecret, totpCode)) {
            throw new InvalidMfaCodeException();
        }

        // Code verified — enable MFA on the account
        redisTemplate.delete(setupKey);
        user.enableMfa(pendingSecret);
        userRepository.save(user);
        log.info("MFA enabled for userId={}", userId);
    }

    @Override
    @Transactional
    public void disable(UUID userId, String totpCode) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (!user.isMfaEnabled()) {
            throw new MfaNotEnabledException();
        }

        if (!codeVerifier.isValidCode(user.getTotpSecret(), totpCode)) {
            throw new InvalidMfaCodeException();
        }

        user.disableMfa();
        userRepository.save(user);
        log.info("MFA disabled for userId={}", userId);
    }

    @Override
    public TokenPair verifyLogin(String pendingMfaToken, String totpCode,
                                 DeviceFingerprint deviceFingerprint) {
        String pendingKey = PENDING_MFA_PREFIX + pendingMfaToken;
        String userIdStr = redisTemplate.opsForValue().get(pendingKey);
        if (userIdStr == null) {
            throw new InvalidPendingMfaTokenException();
        }

        UUID userId = UUID.fromString(userIdStr);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (!codeVerifier.isValidCode(user.getTotpSecret(), totpCode)) {
            throw new InvalidMfaCodeException();
        }

        // Consume the pending token — single-use
        redisTemplate.delete(pendingKey);
        log.info("MFA login verified for userId={}", userId);

        return tokenService.issueTokenPair(user, deviceFingerprint);
    }

    /**
     * Issues a short-lived pending MFA token and stores it in Redis.
     * Called by AuthenticationService when it detects MFA is required.
     */
    public String issuePendingMfaToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        String key = PENDING_MFA_PREFIX + token;
        redisTemplate.opsForValue().set(
            key, userId.toString(),
            Duration.ofSeconds(mfaProperties.pendingTokenTtlSeconds()));
        return token;
    }

    private String buildOtpAuthUri(String email, String secret) {
        String issuer = URLEncoder.encode(mfaProperties.issuer(), StandardCharsets.UTF_8);
        String account = URLEncoder.encode(email, StandardCharsets.UTF_8);
        String encodedSecret = URLEncoder.encode(secret, StandardCharsets.UTF_8);
        return "otpauth://totp/" + issuer + ":" + account
            + "?secret=" + encodedSecret
            + "&issuer=" + issuer
            + "&algorithm=SHA1&digits=6&period=30";
    }
}
