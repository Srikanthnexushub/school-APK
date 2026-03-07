// src/main/java/com/edutech/auth/domain/port/in/VerifyOtpUseCase.java
package com.edutech.auth.domain.port.in;

import com.edutech.auth.application.dto.OtpVerifyRequest;

public interface VerifyOtpUseCase {
    void sendOtp(String email, String purpose, String channel);
    void verifyOtp(OtpVerifyRequest request);
}
