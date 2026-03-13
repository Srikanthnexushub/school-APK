package com.edutech.auth.infrastructure.external;

import com.edutech.auth.domain.port.out.CaptchaVerifier;
import com.edutech.auth.infrastructure.redis.CaptchaRedisStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Server-side CAPTCHA verifier.
 * Expects captchaToken in format "{challengeId}:{userAnswer}".
 * Single-use: the challenge is deleted from Redis after first verification attempt.
 *
 * E2E test bypass: set CAPTCHA_E2E_BYPASS_TOKEN env var to a non-empty secret.
 * When the challengeId equals that secret the token is accepted without Redis lookup.
 * Leave the env var empty (or unset) in all non-test environments.
 */
@Component
public class LocalCaptchaVerifierAdapter implements CaptchaVerifier {

    private static final Logger log = LoggerFactory.getLogger(LocalCaptchaVerifierAdapter.class);

    private final CaptchaRedisStore captchaRedisStore;
    private final String e2eBypassToken;

    public LocalCaptchaVerifierAdapter(CaptchaRedisStore captchaRedisStore,
                                       @Value("${captcha.e2e-bypass-token:}") String e2eBypassToken) {
        this.captchaRedisStore = captchaRedisStore;
        this.e2eBypassToken = e2eBypassToken;
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

        // E2E test bypass — only active when CAPTCHA_E2E_BYPASS_TOKEN is configured
        if (!e2eBypassToken.isBlank() && id.equals(e2eBypassToken)) {
            log.warn("[E2E] CAPTCHA bypass accepted from ip={}", ipAddress);
            return true;
        }

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
