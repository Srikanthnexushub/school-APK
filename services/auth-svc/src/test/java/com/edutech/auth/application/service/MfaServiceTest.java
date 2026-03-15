package com.edutech.auth.application.service;

import com.edutech.auth.application.config.MfaProperties;
import com.edutech.auth.application.exception.InvalidMfaCodeException;
import com.edutech.auth.application.exception.MfaAlreadyEnabledException;
import com.edutech.auth.application.exception.MfaNotEnabledException;
import com.edutech.auth.domain.model.Role;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MfaService unit tests")
class MfaServiceTest {

    @Mock UserRepository userRepository;
    @Mock TokenService tokenService;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    private MfaProperties mfaProperties;
    private MfaService mfaService;
    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        mfaProperties = new MfaProperties("EduTech", 300, 300);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        mfaService = new MfaService(userRepository, tokenService, mfaProperties, redisTemplate);

        userId = UUID.randomUUID();
        activeUser = User.create("user@test.com", "hash", Role.STUDENT, null, "Test", "User", null);
        activeUser.activate();
    }

    @Test
    @DisplayName("initSetup — user without MFA — returns secret and QR URI")
    void initSetup_noMfa_returnsSecretAndQrUri() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        doNothing().when(valueOps).set(anyString(), anyString(), any());

        var response = mfaService.initSetup(userId);

        assertThat(response.secret()).isNotBlank();
        assertThat(response.qrCodeUri()).startsWith("otpauth://totp/");
        assertThat(response.qrCodeUri()).contains("EduTech");
    }

    @Test
    @DisplayName("initSetup — user already has MFA — throws MfaAlreadyEnabledException")
    void initSetup_mfaAlreadyEnabled_throws() {
        activeUser.enableMfa("somesecret");
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> mfaService.initSetup(userId))
            .isInstanceOf(MfaAlreadyEnabledException.class);
    }

    @Test
    @DisplayName("disable — user without MFA — throws MfaNotEnabledException")
    void disable_mfaNotEnabled_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> mfaService.disable(userId, "123456"))
            .isInstanceOf(MfaNotEnabledException.class);
    }

    @Test
    @DisplayName("issuePendingMfaToken — stores token in Redis and returns it")
    void issuePendingMfaToken_storesAndReturns() {
        doNothing().when(valueOps).set(anyString(), anyString(), any());

        String token = mfaService.issuePendingMfaToken(userId);

        assertThat(token).isNotBlank();
    }
}
