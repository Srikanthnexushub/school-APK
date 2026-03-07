// src/test/java/com/edutech/auth/application/service/AuthenticationServiceTest.java
package com.edutech.auth.application.service;

import com.edutech.auth.application.dto.DeviceFingerprint;
import com.edutech.auth.application.dto.LoginRequest;
import com.edutech.auth.application.dto.TokenPair;
import com.edutech.auth.application.exception.AccountLockedException;
import com.edutech.auth.application.exception.CaptchaVerificationException;
import com.edutech.auth.application.exception.InvalidCredentialsException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService unit tests")
class AuthenticationServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasher passwordHasher;
    @Mock CaptchaVerifier captchaVerifier;
    @Mock TokenService tokenService;
    @Mock AuditEventPublisher auditEventPublisher;

    AuthenticationService service;

    private static final String EMAIL    = "student@example.com";
    private static final String PASSWORD = "SecurePass123!";
    private static final String IP       = "10.0.0.1";
    private static final String UA       = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(
            userRepository, passwordHasher, captchaVerifier,
            tokenService, auditEventPublisher
        );
    }

    @Test
    @DisplayName("Returns token pair on successful login")
    void authenticate_success() {
        User user = User.create(EMAIL, "hash", Role.STUDENT, null,
                                "Alice", "Smith", null);
        user.activate();

        DeviceFingerprint fp = new DeviceFingerprint(UA, "device-001", "10.0.0");
        LoginRequest req = new LoginRequest(EMAIL, PASSWORD, "valid-token", fp);
        TokenPair expected = new TokenPair("access", "refresh",
                                           Instant.now().plusSeconds(900),
                                           Instant.now().plusSeconds(604800));

        when(captchaVerifier.verify(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHasher.verify(PASSWORD, "hash")).thenReturn(true);
        when(tokenService.issueTokenPair(user, fp)).thenReturn(expected);

        TokenPair result = service.authenticate(req, IP, UA);

        assertThat(result).isEqualTo(expected);
        verify(auditEventPublisher).publish(any());
    }

    @Test
    @DisplayName("Throws InvalidCredentialsException when user not found")
    void authenticate_userNotFound() {
        DeviceFingerprint fp = new DeviceFingerprint(UA, null, null);
        LoginRequest req = new LoginRequest("unknown@x.com", PASSWORD, "valid-token", fp);

        when(captchaVerifier.verify(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(req, IP, UA))
            .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenService, never()).issueTokenPair(any(), any());
    }

    @Test
    @DisplayName("Throws InvalidCredentialsException on wrong password")
    void authenticate_wrongPassword() {
        User user = User.create(EMAIL, "hash", Role.STUDENT, null, "A", "B", null);
        user.activate();

        DeviceFingerprint fp = new DeviceFingerprint(UA, null, null);
        LoginRequest req = new LoginRequest(EMAIL, "wrong", "valid-token", fp);

        when(captchaVerifier.verify(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHasher.verify("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate(req, IP, UA))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("Throws AccountLockedException for locked user")
    void authenticate_lockedAccount() {
        User user = User.create(EMAIL, "hash", Role.STUDENT, null, "A", "B", null);
        user.activate();
        user.lock("Too many failures");

        DeviceFingerprint fp = new DeviceFingerprint(UA, null, null);
        LoginRequest req = new LoginRequest(EMAIL, PASSWORD, "valid-token", fp);

        when(captchaVerifier.verify(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHasher.verify(PASSWORD, "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.authenticate(req, IP, UA))
            .isInstanceOf(AccountLockedException.class);
    }

    @Test
    @DisplayName("Throws CaptchaVerificationException when captcha fails")
    void authenticate_captchaFailed() {
        DeviceFingerprint fp = new DeviceFingerprint(UA, null, null);
        LoginRequest req = new LoginRequest(EMAIL, PASSWORD, "bad-token", fp);

        when(captchaVerifier.verify(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.authenticate(req, IP, UA))
            .isInstanceOf(CaptchaVerificationException.class);

        verify(userRepository, never()).findByEmail(anyString());
    }
}
