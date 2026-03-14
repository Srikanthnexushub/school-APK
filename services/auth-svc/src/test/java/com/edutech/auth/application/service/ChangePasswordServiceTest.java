package com.edutech.auth.application.service;

import com.edutech.auth.application.exception.IncorrectCurrentPasswordException;
import com.edutech.auth.application.exception.UserNotFoundException;
import com.edutech.auth.domain.model.Role;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.out.PasswordHasher;
import com.edutech.auth.domain.port.out.TokenStore;
import com.edutech.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangePasswordService unit tests")
class ChangePasswordServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasher passwordHasher;
    @Mock TokenStore tokenStore;

    @InjectMocks ChangePasswordService service;

    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = User.create("user@test.com", "hashed-old-pw", Role.STUDENT,
            null, "Test", "User", null);
        activeUser.activate();
    }

    @Test
    @DisplayName("changePassword — correct current password — updates hash and revokes sessions")
    void changePassword_correctCurrentPassword_updatesAndRevokes() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(passwordHasher.verify("old-pw", activeUser.getPasswordHash())).thenReturn(true);
        when(passwordHasher.hash("new-pw")).thenReturn("hashed-new-pw");
        when(userRepository.save(activeUser)).thenReturn(activeUser);

        service.changePassword(userId, "old-pw", "new-pw");

        verify(passwordHasher).hash("new-pw");
        verify(userRepository).save(activeUser);
        verify(tokenStore).deleteAllForUser(userId);
    }

    @Test
    @DisplayName("changePassword — wrong current password — throws IncorrectCurrentPasswordException")
    void changePassword_wrongCurrentPassword_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(passwordHasher.verify(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(userId, "wrong-pw", "new-pw"))
            .isInstanceOf(IncorrectCurrentPasswordException.class);

        verify(userRepository, never()).save(any());
        verify(tokenStore, never()).deleteAllForUser(any());
    }

    @Test
    @DisplayName("changePassword — unknown user — throws UserNotFoundException")
    void changePassword_unknownUser_throws() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePassword(userId, "old-pw", "new-pw"))
            .isInstanceOf(UserNotFoundException.class);
    }
}
