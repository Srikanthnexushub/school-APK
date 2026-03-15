// src/test/java/com/edutech/center/domain/model/TeacherModelTest.java
package com.edutech.center.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Teacher domain model factory methods and state-machine transitions.
 * No Spring context — pure Java.
 */
@DisplayName("Teacher domain model — factories and state transitions")
class TeacherModelTest {

    private static final UUID CENTER_ID = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();

    // ─── Factory: create ──────────────────────────────────────────────────────

    @Test
    @DisplayName("create — produces ACTIVE teacher with all fields set")
    void create_returnsActiveTeacher() {
        Teacher t = Teacher.create(CENTER_ID, USER_ID, "Ravi", "Kumar",
                "ravi@school.com", "+919876543210", "Mathematics");

        assertThat(t.getId()).isNotNull();
        assertThat(t.getCenterId()).isEqualTo(CENTER_ID);
        assertThat(t.getUserId()).isEqualTo(USER_ID);
        assertThat(t.getFirstName()).isEqualTo("Ravi");
        assertThat(t.getLastName()).isEqualTo("Kumar");
        assertThat(t.getEmail()).isEqualTo("ravi@school.com");
        assertThat(t.getStatus()).isEqualTo(TeacherStatus.ACTIVE);
        assertThat(t.getEmployeeId()).isNull();
        assertThat(t.getInvitationToken()).isNull();
        assertThat(t.getInvitationTokenExpiresAt()).isNull();
        assertThat(t.getJoinedAt()).isNotNull();
    }

    // ─── Factory: createInvitationStub ────────────────────────────────────────

    @Test
    @DisplayName("createInvitationStub — produces INVITATION_SENT stub with null userId and token set")
    void createInvitationStub_returnsCorrectStub() {
        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plus(7, ChronoUnit.DAYS);

        Teacher t = Teacher.createInvitationStub(CENTER_ID,
                "Priya", "Singh", "priya@school.com",
                "+919876543211", "Physics,Chemistry",
                "T-001", token, expiry);

        assertThat(t.getId()).isNotNull();
        assertThat(t.getUserId()).isNull();
        assertThat(t.getCenterId()).isEqualTo(CENTER_ID);
        assertThat(t.getStatus()).isEqualTo(TeacherStatus.INVITATION_SENT);
        assertThat(t.getInvitationToken()).isEqualTo(token);
        assertThat(t.getInvitationTokenExpiresAt()).isEqualTo(expiry);
        assertThat(t.getEmployeeId()).isEqualTo("T-001");
        assertThat(t.getSubjects()).isEqualTo("Physics,Chemistry");
    }

    @Test
    @DisplayName("createInvitationStub — nullable fields (phone, employeeId) accepted as null")
    void createInvitationStub_nullableFieldsAccepted() {
        String token = UUID.randomUUID().toString();
        Teacher t = Teacher.createInvitationStub(CENTER_ID,
                "A", "B", "a@b.com",
                null, "English",
                null, token, Instant.now().plus(7, ChronoUnit.DAYS));

        assertThat(t.getPhoneNumber()).isNull();
        assertThat(t.getEmployeeId()).isNull();
        assertThat(t.getStatus()).isEqualTo(TeacherStatus.INVITATION_SENT);
    }

    // ─── Factory: createPending ───────────────────────────────────────────────

    @Test
    @DisplayName("createPending — produces PENDING_APPROVAL teacher with userId set")
    void createPending_returnsPendingTeacher() {
        Teacher t = Teacher.createPending(CENTER_ID, USER_ID,
                "Amit", "Verma", "amit@school.com", null, "History", null);

        assertThat(t.getStatus()).isEqualTo(TeacherStatus.PENDING_APPROVAL);
        assertThat(t.getUserId()).isEqualTo(USER_ID);
        assertThat(t.getInvitationToken()).isNull();
        assertThat(t.getInvitationTokenExpiresAt()).isNull();
    }

    // ─── State transition: acceptInvitation ───────────────────────────────────

    @Test
    @DisplayName("acceptInvitation — links userId, clears token, transitions to ACTIVE")
    void acceptInvitation_success() {
        String token = UUID.randomUUID().toString();
        Teacher t = Teacher.createInvitationStub(CENTER_ID,
                "A", "B", "a@b.com", null, "English",
                null, token, Instant.now().plus(7, ChronoUnit.DAYS));

        UUID newUserId = UUID.randomUUID();
        t.acceptInvitation(newUserId);

        assertThat(t.getUserId()).isEqualTo(newUserId);
        assertThat(t.getStatus()).isEqualTo(TeacherStatus.ACTIVE);
        assertThat(t.getInvitationToken()).isNull();
        assertThat(t.getInvitationTokenExpiresAt()).isNull();
    }

    @Test
    @DisplayName("acceptInvitation — throws IllegalStateException when status is not INVITATION_SENT")
    void acceptInvitation_wrongState_throws() {
        Teacher t = Teacher.createPending(CENTER_ID, USER_ID, "A", "B", "a@b.com", null, "Hindi", null);

        assertThatThrownBy(() -> t.acceptInvitation(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invitation already used or invalid");
    }

    @Test
    @DisplayName("acceptInvitation — throws when called twice on same stub")
    void acceptInvitation_calledTwice_throws() {
        String token = UUID.randomUUID().toString();
        Teacher t = Teacher.createInvitationStub(CENTER_ID,
                "A", "B", "a@b.com", null, "English",
                null, token, Instant.now().plus(7, ChronoUnit.DAYS));
        t.acceptInvitation(UUID.randomUUID());

        assertThatThrownBy(() -> t.acceptInvitation(UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ─── State transition: approve ────────────────────────────────────────────

    @Test
    @DisplayName("approve — transitions PENDING_APPROVAL to ACTIVE")
    void approve_success() {
        Teacher t = Teacher.createPending(CENTER_ID, USER_ID, "A", "B", "a@b.com", null, "Geography", null);

        t.approve();

        assertThat(t.getStatus()).isEqualTo(TeacherStatus.ACTIVE);
        assertThat(t.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("approve — throws IllegalStateException when status is not PENDING_APPROVAL")
    void approve_wrongState_throws() {
        Teacher t = Teacher.create(CENTER_ID, USER_ID, "A", "B", "a@b.com", null, "Music");

        assertThatThrownBy(t::approve)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending approval");
    }

    // ─── State transition: reject ─────────────────────────────────────────────

    @Test
    @DisplayName("reject — transitions PENDING_APPROVAL to INACTIVE and sets deletedAt")
    void reject_success() {
        Teacher t = Teacher.createPending(CENTER_ID, USER_ID, "A", "B", "a@b.com", null, "Economics", null);

        t.reject();

        assertThat(t.getStatus()).isEqualTo(TeacherStatus.INACTIVE);
        assertThat(t.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("reject — throws IllegalStateException when status is not PENDING_APPROVAL")
    void reject_wrongState_throws() {
        Teacher t = Teacher.create(CENTER_ID, USER_ID, "A", "B", "a@b.com", null, "Fine Arts");

        assertThatThrownBy(t::reject)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending approval");
    }

    // ─── isActive ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isActive — true only for ACTIVE status with no deletedAt")
    void isActive_trueForActiveOnly() {
        Teacher active  = Teacher.create(CENTER_ID, USER_ID, "A", "B", "a@b.com", null, "Biology");
        Teacher pending = Teacher.createPending(CENTER_ID, USER_ID, "A", "B", "b@b.com", null, "Physics", null);

        assertThat(active.isActive()).isTrue();
        assertThat(pending.isActive()).isFalse();
    }
}
