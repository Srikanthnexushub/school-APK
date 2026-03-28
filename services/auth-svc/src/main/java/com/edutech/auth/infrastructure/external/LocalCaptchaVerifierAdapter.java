package com.edutech.auth.infrastructure.external;

import com.edutech.auth.domain.port.out.CaptchaVerifier;
import com.edutech.auth.infrastructure.redis.CaptchaRedisStore;
import jakarta.annotation.PostConstruct;
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
 *
 * PRODUCTION GUARD: if APP_ENVIRONMENT contains "prod" and the bypass token is
 * non-empty, the application will REFUSE TO START. This is a hard enforcement
 * rule — see docs/incidents/captcha-production-bypass/INCIDENT-REPORT.md.
 */
@Component
public class LocalCaptchaVerifierAdapter implements CaptchaVerifier {

    private static final Logger log = LoggerFactory.getLogger(LocalCaptchaVerifierAdapter.class);

    private final CaptchaRedisStore captchaRedisStore;
    private final String e2eBypassToken;
    private final String appEnvironment;

    public LocalCaptchaVerifierAdapter(CaptchaRedisStore captchaRedisStore,
                                       @Value("${captcha.e2e-bypass-token:}") String e2eBypassToken,
                                       @Value("${APP_ENVIRONMENT:local}") String appEnvironment) {
        this.captchaRedisStore = captchaRedisStore;
        this.e2eBypassToken = e2eBypassToken;
        this.appEnvironment = appEnvironment;
    }

    /**
     * Startup guard: refuse to start if CAPTCHA bypass token is configured in production.
     * This prevents the incident documented in docs/incidents/captcha-production-bypass/
     * from ever recurring. No override, no workaround — fix the env var instead.
     */
    @PostConstruct
    public void validateBypassTokenNotSetInProduction() {
        if (!e2eBypassToken.isBlank() && appEnvironment.toLowerCase().contains("prod")) {
            throw new IllegalStateException(
                "[SECURITY] CAPTCHA E2E bypass token must NOT be set in production environments. " +
                "APP_ENVIRONMENT='" + appEnvironment + "' detected as production but " +
                "CAPTCHA_E2E_BYPASS_TOKEN is non-empty. " +
                "Clear CAPTCHA_E2E_BYPASS_TOKEN in /opt/apps/.env.prod and restart. " +
                "See docs/incidents/captcha-production-bypass/INCIDENT-REPORT.md"
            );
        }
        if (!e2eBypassToken.isBlank()) {
            log.warn("[SECURITY] CAPTCHA E2E bypass token is active (env={}). " +
                     "This must ONLY be used in local/test environments.", appEnvironment);
        }
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
