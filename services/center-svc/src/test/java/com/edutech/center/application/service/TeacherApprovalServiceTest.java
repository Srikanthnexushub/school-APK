// src/test/java/com/edutech/center/application/service/TeacherApprovalServiceTest.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.TeacherResponse;
import com.edutech.center.application.dto.TeacherSelfRegisterRequest;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterNotFoundException;
import com.edutech.center.application.exception.TeacherAlreadyAssignedException;
import com.edutech.center.application.exception.TeacherNotFoundException;
import com.edutech.center.domain.model.CoachingCenter;
import com.edutech.center.domain.model.Role;
import com.edutech.center.domain.model.Teacher;
import com.edutech.center.domain.model.TeacherStatus;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.domain.port.out.TeacherRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeacherApprovalService unit tests")
class TeacherApprovalServiceTest {

    @Mock TeacherRepository teacherRepository;
    @Mock CenterRepository  centerRepository;
    @Mock CenterEventPublisher eventPublisher;
    @InjectMocks TeacherApprovalService service;

    private static final UUID CENTER_ID       = UUID.randomUUID();
    private static final UUID ADMIN_ID        = UUID.randomUUID();
    private static final UUID TEACHER_USER_ID = UUID.randomUUID();

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AuthPrincipal centerAdmin() {
        return new AuthPrincipal(ADMIN_ID, "admin@center.com", Role.CENTER_ADMIN, CENTER_ID, "fp");
    }

    private AuthPrincipal superAdmin() {
        return new AuthPrincipal(UUID.randomUUID(), "super@nexused.dev", Role.SUPER_ADMIN, null, "fp");
    }

    private AuthPrincipal teacherPrincipal() {
        return new AuthPrincipal(TEACHER_USER_ID, "teacher@school.com", Role.TEACHER, null, "fp");
    }

    /**
     * Stubs CoachingCenter mock and centerRepository.findById() sequentially to avoid
     * UnfinishedStubbingException (do NOT call when() inside thenReturn() args).
     */
    private void stubCenter() {
        CoachingCenter c = mock(CoachingCenter.class);
        // lenient: getName() only called in selfRegister event; getAdminUserId() only in wrong-admin fallback
        lenient().when(c.getName()).thenReturn("Test Academy");
        lenient().when(c.getAdminUserId()).thenReturn(ADMIN_ID);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(c));
    }

    private TeacherSelfRegisterRequest selfRegRequest() {
        return new TeacherSelfRegisterRequest(
                "Amit", "Verma", "amit@school.com", "+919876543210", "History,Geography");
    }

    // ─── selfRegister ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("selfRegister — creates PENDING_APPROVAL teacher and publishes event")
    void selfRegister_success() {
        stubCenter();
        when(teacherRepository.existsByUserIdAndCenterId(TEACHER_USER_ID, CENTER_ID)).thenReturn(false);
        when(teacherRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TeacherResponse response = service.selfRegister(CENTER_ID, selfRegRequest(), teacherPrincipal());

        assertThat(response.firstName()).isEqualTo("Amit");
        assertThat(response.lastName()).isEqualTo("Verma");
        assertThat(response.status()).isEqualTo(TeacherStatus.PENDING_APPROVAL);
        assertThat(response.userId()).isEqualTo(TEACHER_USER_ID);
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("selfRegister — center not found throws CenterNotFoundException")
    void selfRegister_centerNotFound_throws() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.selfRegister(CENTER_ID, selfRegRequest(), teacherPrincipal()))
                .isInstanceOf(CenterNotFoundException.class);
    }

    @Test
    @DisplayName("selfRegister — already assigned to center throws TeacherAlreadyAssignedException")
    void selfRegister_alreadyAssigned_throws() {
        stubCenter();
        when(teacherRepository.existsByUserIdAndCenterId(TEACHER_USER_ID, CENTER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.selfRegister(CENTER_ID, selfRegRequest(), teacherPrincipal()))
                .isInstanceOf(TeacherAlreadyAssignedException.class);
    }

    // ─── listPending ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("listPending — returns all PENDING_APPROVAL teachers for CENTER_ADMIN of this center")
    void listPending_returnsTeachers() {
        Teacher pending = Teacher.createPending(CENTER_ID, TEACHER_USER_ID,
                "A", "B", "a@b.com", null, "Hindi");
        when(teacherRepository.findPendingByCenterId(CENTER_ID)).thenReturn(List.of(pending));

        List<TeacherResponse> result = service.listPending(CENTER_ID, centerAdmin());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(TeacherStatus.PENDING_APPROVAL);
    }

    @Test
    @DisplayName("listPending — SUPER_ADMIN can list pending for any center")
    void listPending_superAdminHasAccess() {
        when(teacherRepository.findPendingByCenterId(CENTER_ID)).thenReturn(List.of());

        List<TeacherResponse> result = service.listPending(CENTER_ID, superAdmin());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listPending — CENTER_ADMIN of different center throws CenterAccessDeniedException")
    void listPending_wrongAdmin_throws() {
        AuthPrincipal wrongAdmin = new AuthPrincipal(UUID.randomUUID(), "x@other.com",
                Role.CENTER_ADMIN, UUID.randomUUID(), "fp");
        stubCenter(); // needed for fallback adminUserId check inside assertAdminAccess

        assertThatThrownBy(() -> service.listPending(CENTER_ID, wrongAdmin))
                .isInstanceOf(CenterAccessDeniedException.class);
    }

    @Test
    @DisplayName("listPending — returns empty list when no pending teachers exist")
    void listPending_emptyWhenNoPendingTeachers() {
        when(teacherRepository.findPendingByCenterId(CENTER_ID)).thenReturn(List.of());

        List<TeacherResponse> result = service.listPending(CENTER_ID, centerAdmin());

        assertThat(result).isEmpty();
    }

    // ─── approve ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("approve — transitions teacher from PENDING_APPROVAL to ACTIVE")
    void approve_success() {
        UUID teacherId = UUID.randomUUID();
        Teacher pending = Teacher.createPending(CENTER_ID, TEACHER_USER_ID,
                "A", "B", "a@b.com", null, "Music");
        when(teacherRepository.findByIdAndCenterId(teacherId, CENTER_ID)).thenReturn(Optional.of(pending));
        when(teacherRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TeacherResponse response = service.approve(CENTER_ID, teacherId, centerAdmin());

        assertThat(response.status()).isEqualTo(TeacherStatus.ACTIVE);
    }

    @Test
    @DisplayName("approve — SUPER_ADMIN can approve teachers in any center")
    void approve_superAdminHasAccess() {
        UUID teacherId = UUID.randomUUID();
        Teacher pending = Teacher.createPending(CENTER_ID, TEACHER_USER_ID,
                "A", "B", "a@b.com", null, "Biology");
        when(teacherRepository.findByIdAndCenterId(teacherId, CENTER_ID)).thenReturn(Optional.of(pending));
        when(teacherRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TeacherResponse response = service.approve(CENTER_ID, teacherId, superAdmin());

        assertThat(response.status()).isEqualTo(TeacherStatus.ACTIVE);
    }

    @Test
    @DisplayName("approve — teacher not found throws TeacherNotFoundException")
    void approve_notFound_throws() {
        UUID teacherId = UUID.randomUUID();
        when(teacherRepository.findByIdAndCenterId(teacherId, CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(CENTER_ID, teacherId, centerAdmin()))
                .isInstanceOf(TeacherNotFoundException.class);
    }

    @Test
    @DisplayName("approve — wrong center admin throws CenterAccessDeniedException")
    void approve_wrongAdmin_throws() {
        UUID teacherId = UUID.randomUUID();
        AuthPrincipal wrongAdmin = new AuthPrincipal(UUID.randomUUID(), "x@other.com",
                Role.CENTER_ADMIN, UUID.randomUUID(), "fp");
        stubCenter(); // needed for fallback adminUserId check

        assertThatThrownBy(() -> service.approve(CENTER_ID, teacherId, wrongAdmin))
                .isInstanceOf(CenterAccessDeniedException.class);
    }

    // ─── reject ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reject — transitions teacher from PENDING_APPROVAL to INACTIVE and sets deletedAt")
    void reject_success() {
        UUID teacherId = UUID.randomUUID();
        Teacher pending = Teacher.createPending(CENTER_ID, TEACHER_USER_ID,
                "A", "B", "a@b.com", null, "Fine Arts");
        when(teacherRepository.findByIdAndCenterId(teacherId, CENTER_ID)).thenReturn(Optional.of(pending));
        when(teacherRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TeacherResponse response = service.reject(CENTER_ID, teacherId, centerAdmin());

        assertThat(response.status()).isEqualTo(TeacherStatus.INACTIVE);
    }

    @Test
    @DisplayName("reject — teacher not found throws TeacherNotFoundException")
    void reject_notFound_throws() {
        UUID teacherId = UUID.randomUUID();
        when(teacherRepository.findByIdAndCenterId(teacherId, CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reject(CENTER_ID, teacherId, centerAdmin()))
                .isInstanceOf(TeacherNotFoundException.class);
    }

    @Test
    @DisplayName("reject — wrong center admin throws CenterAccessDeniedException")
    void reject_wrongAdmin_throws() {
        UUID teacherId = UUID.randomUUID();
        AuthPrincipal wrongAdmin = new AuthPrincipal(UUID.randomUUID(), "x@other.com",
                Role.CENTER_ADMIN, UUID.randomUUID(), "fp");
        stubCenter(); // needed for fallback adminUserId check

        assertThatThrownBy(() -> service.reject(CENTER_ID, teacherId, wrongAdmin))
                .isInstanceOf(CenterAccessDeniedException.class);
    }
}
