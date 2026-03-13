// src/test/java/com/edutech/auth/application/service/UserCleanupSchedulerTest.java
package com.edutech.auth.application.service;

import com.edutech.auth.domain.model.Role;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.out.UserRepository;
import com.edutech.auth.infrastructure.config.CleanupProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCleanupScheduler unit tests")
class UserCleanupSchedulerTest {

    @Mock UserRepository userRepository;

    CleanupProperties props;
    UserCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        props = new CleanupProperties("0 0 * * * *", 72);
        scheduler = new UserCleanupScheduler(userRepository, props);
    }

    @Test
    @DisplayName("No expired users — save is never called")
    void purge_noExpiredUsers_logsDebug() {
        when(userRepository.findExpiredPendingVerification(any(Instant.class)))
            .thenReturn(List.of());

        scheduler.purgeExpiredUnverifiedAccounts();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Expired users are soft-deleted and saved")
    void purge_expiredUsers_softDeletesAndSaves() {
        User user1 = User.create("a@example.com", "hash1", Role.STUDENT, null, "Alice", "A", null);
        User user2 = User.create("b@example.com", "hash2", Role.STUDENT, null, "Bob", "B", null);

        when(userRepository.findExpiredPendingVerification(any(Instant.class)))
            .thenReturn(List.of(user1, user2));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        scheduler.purgeExpiredUnverifiedAccounts();

        verify(userRepository, times(2)).save(any(User.class));
        assertThat(user1.getDeletedAt()).isNotNull();
        assertThat(user2.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Cutoff passed to repo matches now() minus retentionHours")
    void purge_usesCorrectCutoffFromConfig() {
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        when(userRepository.findExpiredPendingVerification(cutoffCaptor.capture()))
            .thenReturn(List.of());

        Instant before = Instant.now().minus(props.retentionHours(), ChronoUnit.HOURS);
        scheduler.purgeExpiredUnverifiedAccounts();
        Instant after = Instant.now().minus(props.retentionHours(), ChronoUnit.HOURS);

        Instant captured = cutoffCaptor.getValue();
        assertThat(captured).isBetween(before, after);
    }
}
