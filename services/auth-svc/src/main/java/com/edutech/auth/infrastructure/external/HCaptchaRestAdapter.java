// src/main/java/com/edutech/auth/infrastructure/external/HCaptchaRestAdapter.java
package com.edutech.auth.infrastructure.external;

import com.edutech.auth.domain.port.out.CaptchaVerifier;
import com.edutech.auth.infrastructure.config.CaptchaProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * External hCaptcha/Google reCAPTCHA REST adapter — kept for reference but disabled.
 * The active CaptchaVerifier bean is LocalCaptchaVerifierAdapter (server-generated image CAPTCHA).
 * Re-enable by adding @Component and removing it from LocalCaptchaVerifierAdapter.
 */
public class HCaptchaRestAdapter implements CaptchaVerifier {

    private static final Logger log = LoggerFactory.getLogger(HCaptchaRestAdapter.class);

    private final RestClient restClient;
    private final CaptchaProperties captchaProperties;

    public HCaptchaRestAdapter(RestClient.Builder restClientBuilder,
                               CaptchaProperties captchaProperties) {
        this.captchaProperties = captchaProperties;
        this.restClient = restClientBuilder
            .baseUrl(captchaProperties.verifyUrl())
            .build();
    }

    @Override
    @CircuitBreaker(name = "captcha-client", fallbackMethod = "captchaFallback")
    public boolean verify(String captchaToken, String ipAddress) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", captchaProperties.secretKey());
        body.add("response", captchaToken);
        body.add("remoteip", ipAddress);

        HCaptchaResponse response = restClient.post()
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(HCaptchaResponse.class);

        if (response == null) return false;

        if (!response.success()) {
            log.warn("hCaptcha verification failed: errors={}", response.errorCodes());
        }
        return response.success();
    }

    @SuppressWarnings("unused")
    boolean captchaFallback(String captchaToken, String ipAddress, Throwable ex) {
        log.error("hCaptcha circuit breaker open — rejecting request. Cause: {}", ex.getMessage());
        return false;
    }
}
