// src/main/java/com/edutech/auth/api/OtpController.java
package com.edutech.auth.api;

import com.edutech.auth.application.dto.OtpSendRequest;
import com.edutech.auth.application.dto.OtpSendResponse;
import com.edutech.auth.application.dto.OtpVerifyRequest;
import com.edutech.auth.domain.port.in.VerifyOtpUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/otp")
@Tag(name = "OTP", description = "One-time password send and verify endpoints")
public class OtpController {

    private final VerifyOtpUseCase verifyOtpUseCase;

    public OtpController(VerifyOtpUseCase verifyOtpUseCase) {
        this.verifyOtpUseCase = verifyOtpUseCase;
    }

    @PostMapping("/send")
    @Operation(summary = "Send OTP to the specified email or phone")
    public OtpSendResponse send(@Valid @RequestBody OtpSendRequest request) {
        return verifyOtpUseCase.sendOtp(request.email(), request.purpose(), request.channel());
    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Verify OTP — activates account if purpose is EMAIL_VERIFICATION")
    public void verify(@Valid @RequestBody OtpVerifyRequest request) {
        verifyOtpUseCase.verifyOtp(request);
    }
}
