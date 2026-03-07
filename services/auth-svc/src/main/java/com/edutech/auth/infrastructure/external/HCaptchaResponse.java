// src/main/java/com/edutech/auth/infrastructure/external/HCaptchaResponse.java
package com.edutech.auth.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Immutable response from hCaptcha Enterprise verify API.
 */
record HCaptchaResponse(
    boolean success,
    @JsonProperty("error-codes") List<String> errorCodes,
    String hostname,
    @JsonProperty("challenge_ts") String challengeTs
) {}
