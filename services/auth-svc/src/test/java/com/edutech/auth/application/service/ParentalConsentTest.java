// src/test/java/com/edutech/auth/application/service/ParentalConsentTest.java
package com.edutech.auth.application.service;

import com.edutech.auth.application.dto.DeviceFingerprint;
import com.edutech.auth.application.dto.RegisterRequest;
import com.edutech.auth.application.dto.TokenPair;
import com.edutech.auth.domain.model.Role;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.out.AuditEventPublisher;
import com.edutech.auth.domain.port.out.CaptchaVerifier;
import com.edutech.auth.domain.port.out.PasswordHasher;
import com.edutech.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParentalConsentTest — under-13 consent email gate")
class ParentalConsentTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasher passwordHasher;
    @Mock CaptchaVerifier captchaVerifier;
    @Mock TokenService tokenService;
    @Mock OtpService otpService;
    @Mock AuditEventPublisher auditEventPublisher;

    UserRegistrationService service;

    private static final String EMAIL        = "young-student@example.com";
    private static final String PASSWORD     = "SecurePass123!";
    private static final String PARENT_EMAIL = "parent@example.com";
    private static final String IP           = "10.0.0.1";
    private static final String UA           = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        service = new UserRegistrationService(
            userRepository, passwordHasher, captchaVerifier,
            tokenService, otpService, auditEventPublisher
        );
    }

    @Test
    @DisplayName("register_under13_sendsConsentEmail — parentEmail provided triggers PARENTAL_CONSENT OTP")
    void register_under13_sendsConsentEmail() {
        DeviceFingerprint fp = new DeviceFingerprint(UA, "device-001", "10.0.0");
        RegisterRequest req = new RegisterRequest(
            EMAIL, PASSWORD, Role.STUDENT, null,
            "Jane", "Doe", null, "valid-captcha", fp, PARENT_EMAIL
        );

        User savedUser = User.create(EMAIL, "hash", Role.STUDENT, null, "Jane", "Doe", null, PARENT_EMAIL);
        TokenPair tokenPair = new TokenPair("access", "refresh",
                                            Instant.now().plusSeconds(900),
                                            Instant.now().plusSeconds(604800));

        when(captchaVerifier.verify(anyString(), anyString())).thenReturn(true);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordHasher.hash(PASSWORD)).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenService.issueTokenPair(any(User.class), eq(fp))).thenReturn(tokenPair);

        service.register(req, IP, UA);

        // Verify EMAIL_VERIFICATION OTP sent to student
        verify(otpService).sendOtp(EMAIL, "EMAIL_VERIFICATION", "email");

        // Verify PARENTAL_CONSENT OTP sent to parent email
        verify(otpService).sendOtp(PARENT_EMAIL, "PARENTAL_CONSENT", "email");
    }

    @Test
    @DisplayName("register_over13_noConsentEmail — parentEmail null does NOT trigger consent OTP")
    void register_over13_noConsentEmail() {
        DeviceFingerprint fp = new DeviceFingerprint(UA, "device-002", "10.0.0");
        RegisterRequest req = new RegisterRequest(
            EMAIL, PASSWORD, Role.STUDENT, null,
            "John", "Doe", null, "valid-captcha", fp, null
        );

        User savedUser = User.create(EMAIL, "hash", Role.STUDENT, null, "John", "Doe", null, null);
        TokenPair tokenPair = new TokenPair("access", "refresh",
                                            Instant.now().plusSeconds(900),
                                            Instant.now().plusSeconds(604800));

        when(captchaVerifier.verify(anyString(), anyString())).thenReturn(true);
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordHasher.hash(PASSWORD)).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenService.issueTokenPair(any(User.class), eq(fp))).thenReturn(tokenPair);

        service.register(req, IP, UA);

        // Verify EMAIL_VERIFICATION OTP sent to student
        verify(otpService).sendOtp(EMAIL, "EMAIL_VERIFICATION", "email");

        // Verify NO consent OTP sent for any purpose related to PARENTAL_CONSENT
        verify(otpService, never()).sendOtp(anyString(), eq("PARENTAL_CONSENT"), anyString());
    }
}
