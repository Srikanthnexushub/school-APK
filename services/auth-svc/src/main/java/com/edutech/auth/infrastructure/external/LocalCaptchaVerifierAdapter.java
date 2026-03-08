package com.edutech.auth.infrastructure.external;

import com.edutech.auth.domain.port.out.CaptchaVerifier;
import com.edutech.auth.infrastructure.redis.CaptchaRedisStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Server-side CAPTCHA verifier.
 * Expects captchaToken in format "{challengeId}:{userAnswer}".
 * Single-use: the challenge is deleted from Redis after first verification attempt.
 */
@Component
public class LocalCaptchaVerifierAdapter implements CaptchaVerifier {

    private static final Logger log = LoggerFactory.getLogger(LocalCaptchaVerifierAdapter.class);

    private final CaptchaRedisStore captchaRedisStore;

    public LocalCaptchaVerifierAdapter(CaptchaRedisStore captchaRedisStore) {
        this.captchaRedisStore = captchaRedisStore;
    }

    @Override
    public boolean verify(String captchaToken, String ipAddress) {
        if (captchaToken == null || !captchaToken.contains(":")) {
            log.warn("CAPTCHA token missing or malformed from ip={}", ipAddress);
            return false;
        }
        int sep = captchaToken.indexOf(':');
        String id = captchaToken.substring(0, sep);
        String userAnswer = captchaToken.substring(sep + 1);

        String stored = captchaRedisStore.findAndDelete(id);
        if (stored == null) {
            log.warn("CAPTCHA challenge not found or expired: id={} ip={}", id, ipAddress);
            return false;
        }
        boolean valid = stored.equalsIgnoreCase(userAnswer.trim());
        if (!valid) {
            log.warn("CAPTCHA answer incorrect: id={} ip={}", id, ipAddress);
        }
        return valid;
    }
}
