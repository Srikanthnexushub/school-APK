// src/main/java/com/edutech/auth/domain/port/in/VerifyOtpUseCase.java
package com.edutech.auth.domain.port.in;

import com.edutech.auth.application.dto.OtpSendResponse;
import com.edutech.auth.application.dto.OtpVerifyRequest;
import com.edutech.auth.application.dto.TokenPair;

import java.util.Optional;

public interface VerifyOtpUseCase {
    OtpSendResponse sendOtp(String email, String purpose, String channel);
    /** Verifies OTP. Returns a TokenPair when purpose=EMAIL_VERIFICATION (auto-login after activation). */
    Optional<TokenPair> verifyOtp(OtpVerifyRequest request);
}
