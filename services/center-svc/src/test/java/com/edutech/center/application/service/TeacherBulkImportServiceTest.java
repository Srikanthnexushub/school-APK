// src/test/java/com/edutech/center/application/service/TeacherBulkImportServiceTest.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BulkImportConfirmResponse;
import com.edutech.center.application.dto.BulkImportPreviewResponse;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterNotFoundException;
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
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeacherBulkImportService unit tests")
class TeacherBulkImportServiceTest {

    @Mock TeacherRepository teacherRepository;
    @Mock CenterRepository  centerRepository;
    @Mock CenterEventPublisher eventPublisher;
    @InjectMocks TeacherBulkImportService service;

    private static final UUID CENTER_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID  = UUID.randomUUID();

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AuthPrincipal centerAdmin() {
        return new AuthPrincipal(ADMIN_ID, "admin@center.com", Role.CENTER_ADMIN, CENTER_ID, "fp");
    }

    /**
     * Stubs both the CoachingCenter mock and the repository lookup in a single
     * sequential chain — avoids the UnfinishedStubbingException that occurs when
     * when() is called inside a thenReturn() argument.
     */
    private void stubCenter() {
        CoachingCenter c = mock(CoachingCenter.class);
        // lenient: getName() is only called in confirm(); getAdminUserId() only in wrong-admin fallback
        lenient().when(c.getName()).thenReturn("Test Academy");
        lenient().when(c.getAdminUserId()).thenReturn(ADMIN_ID);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(c));
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "teachers.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }

    private static final String VALID_CSV =
            "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
            "Ravi,Kumar,ravi@school.com,+919876543210,Mathematics,T-001\n" +
            "Priya,Singh,priya@school.com,+919876543211,Physics,T-002\n";

    private void stubNoExistingTeachers() {
        when(teacherRepository.existsByEmailAndCenterId(anyString(), any())).thenReturn(false);
        when(teacherRepository.existsByEmail(anyString())).thenReturn(false);
    }

    // ─── preview — happy path ─────────────────────────────────────────────────

    @Test
    @DisplayName("preview — valid CSV returns correct row counts with no errors")
    void preview_validCsv_noErrors() {
        stubCenter();
        stubNoExistingTeachers();

        BulkImportPreviewResponse result = service.preview(CENTER_ID, csv(VALID_CSV), centerAdmin());

        assertThat(result.totalRows()).isEqualTo(2);
        assertThat(result.validRows()).isEqualTo(2);
        assertThat(result.errorRows()).isEqualTo(0);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("preview — header line is skipped and not counted as a data row")
    void preview_headerLineSkipped() {
        stubCenter();
        stubNoExistingTeachers();

        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,ravi@school.com,+91987,Mathematics,T-001\n";
        BulkImportPreviewResponse result = service.preview(CENTER_ID, csv(csv), centerAdmin());

        assertThat(result.totalRows()).isEqualTo(1);
    }

    @Test
    @DisplayName("preview — quoted subjects containing commas are parsed as one field")
    void preview_quotedSubjects_parsedCorrectly() {
        stubCenter();
        stubNoExistingTeachers();

        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,ravi@school.com,+91987,\"Mathematics,Physics\",T-001\n";
        BulkImportPreviewResponse result = service.preview(CENTER_ID, csv(csv), centerAdmin());

        assertThat(result.totalRows()).isEqualTo(1);
        assertThat(result.errorRows()).isEqualTo(0);
    }

    // ─── preview — validation errors ──────────────────────────────────────────

    @Test
    @DisplayName("preview — missing first name produces firstName field error")
    void preview_missingFirstName_error() {
        stubCenter();
        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     ",Kumar,ravi@school.com,+91987,Mathematics,T-001\n";

        BulkImportPreviewResponse result = service.preview(CENTER_ID, csv(csv), centerAdmin());

        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.errors().get(0).field()).isEqualTo("firstName");
    }

    @Test
    @DisplayName("preview — invalid email format produces email field error")
    void preview_invalidEmail_error() {
        stubCenter();
        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,not-an-email,+91987,Mathematics,T-001\n";

        BulkImportPreviewResponse result = service.preview(CENTER_ID, csv(csv), centerAdmin());

        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.errors().get(0).field()).isEqualTo("email");
        assertThat(result.errors().get(0).message()).contains("Invalid email");
    }

    @Test
    @DisplayName("preview — duplicate email within file produces error on second row only")
    void preview_duplicateEmailInFile_errorOnSecondRow() {
        stubCenter();
        stubNoExistingTeachers();
        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,same@school.com,+91987,Mathematics,T-001\n" +
                     "Priya,Singh,same@school.com,+91988,Physics,T-002\n";

        BulkImportPreviewResponse result = service.preview(CENTER_ID, csv(csv), centerAdmin());

        assertThat(result.validRows()).isEqualTo(1);
        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.errors()).anyMatch(e -> e.message().contains("Duplicate email"));
    }

    @Test
    @DisplayName("preview — already-registered email produces 'already registered' error")
    void preview_alreadyRegisteredEmail_error() {
        stubCenter();
        when(teacherRepository.existsByEmail("ravi@school.com")).thenReturn(true);
        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,ravi@school.com,+91987,Mathematics,T-001\n";

        BulkImportPreviewResponse result = service.preview(CENTER_ID, csv(csv), centerAdmin());

        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.errors().get(0).message()).contains("already registered");
    }

    @Test
    @DisplayName("preview — unrecognized subject produces subjects error with 'did you mean?' suggestion")
    void preview_unknownSubject_errorWithSuggestion() {
        stubCenter();
        stubNoExistingTeachers();
        // "Mathss" typo — 'mathss'.contains('maths') alias → suggestion "Mathematics"
        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,ravi@school.com,+91987,Mathss,T-001\n";

        BulkImportPreviewResponse result = service.preview(CENTER_ID, csv(csv), centerAdmin());

        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.errors().get(0).field()).isEqualTo("subjects");
        assertThat(result.errors().get(0).suggestion()).isNotNull();
        assertThat(result.errors().get(0).suggestion()).contains("Mathematics");
    }

    @Test
    @DisplayName("preview — completely unknown subject produces subjects error without suggestion")
    void preview_completelyUnknownSubject_noSuggestion() {
        stubCenter();
        stubNoExistingTeachers();
        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,ravi@school.com,+91987,Astrology,T-001\n";

        BulkImportPreviewResponse result = service.preview(CENTER_ID, csv(csv), centerAdmin());

        assertThat(result.errorRows()).isEqualTo(1);
        assertThat(result.errors().get(0).field()).isEqualTo("subjects");
        assertThat(result.errors().get(0).suggestion()).isNull();
    }

    @Test
    @DisplayName("preview — mixed valid and invalid rows return combined counts")
    void preview_mixedRows_combinedCounts() {
        stubCenter();
        stubNoExistingTeachers();
        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,bad-email,+91987,Mathematics,T-001\n" +
                     "Priya,Singh,priya@school.com,+91988,Physics,T-002\n" +
                     "Anita,Rao,anita@school.com,+91989,Chemistry,T-003\n";

        BulkImportPreviewResponse result = service.preview(CENTER_ID, csv(csv), centerAdmin());

        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.validRows()).isEqualTo(2);
        assertThat(result.errorRows()).isEqualTo(1);
    }

    // ─── preview — access control ─────────────────────────────────────────────

    @Test
    @DisplayName("preview — center not found throws CenterNotFoundException")
    void preview_centerNotFound_throws() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preview(CENTER_ID, csv(VALID_CSV), centerAdmin()))
                .isInstanceOf(CenterNotFoundException.class);
    }

    @Test
    @DisplayName("preview — CENTER_ADMIN of different center throws CenterAccessDeniedException")
    void preview_wrongCenterAdmin_throws() {
        AuthPrincipal wrongAdmin = new AuthPrincipal(UUID.randomUUID(), "x@x.com",
                Role.CENTER_ADMIN, UUID.randomUUID(), "fp");
        // stubCenter so assertAccess can look up adminUserId (ADMIN_ID) and compare vs wrongAdmin userId
        stubCenter();

        assertThatThrownBy(() -> service.preview(CENTER_ID, csv(VALID_CSV), wrongAdmin))
                .isInstanceOf(CenterAccessDeniedException.class);
    }

    // ─── confirm — happy path ─────────────────────────────────────────────────

    @Test
    @DisplayName("confirm — valid CSV creates teacher stubs, saves all, publishes one event per teacher")
    void confirm_validCsv_savesAndPublishesEvents() {
        stubCenter();
        stubNoExistingTeachers();

        BulkImportConfirmResponse result = service.confirm(CENTER_ID, csv(VALID_CSV), false, centerAdmin());

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(0);
        assertThat(result.message()).contains("2");
        verify(teacherRepository).saveAll(any());
        verify(eventPublisher, times(2)).publish(any());
    }

    @Test
    @DisplayName("confirm — skipErrors=true skips invalid rows and saves only valid ones")
    void confirm_skipErrors_savesValidOnly() {
        stubCenter();
        when(teacherRepository.existsByEmailAndCenterId(anyString(), any())).thenReturn(false);
        when(teacherRepository.existsByEmail("priya@school.com")).thenReturn(false);
        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,not-an-email,+91987,Mathematics,T-001\n" +
                     "Priya,Singh,priya@school.com,+91988,Physics,T-002\n";

        BulkImportConfirmResponse result = service.confirm(CENTER_ID, csv(csv), true, centerAdmin());

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        verify(eventPublisher, times(1)).publish(any());
    }

    // ─── confirm — error handling ─────────────────────────────────────────────

    @Test
    @DisplayName("confirm — skipErrors=false with CSV errors throws IllegalArgumentException")
    void confirm_withErrors_skipFalse_throws() {
        stubCenter();
        String csv = "First Name,Last Name,Email,Phone,Subjects,Employee ID\n" +
                     "Ravi,Kumar,bad-email,+91987,Mathematics,T-001\n";

        assertThatThrownBy(() -> service.confirm(CENTER_ID, csv(csv), false, centerAdmin()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("error(s)");
        verify(teacherRepository, never()).saveAll(any());
    }

    // ─── findByToken ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByToken — valid non-expired token returns the teacher")
    void findByToken_validToken_returnsTeacher() {
        String token = UUID.randomUUID().toString();
        Teacher stub = Teacher.createInvitationStub(CENTER_ID, "A", "B", "a@b.com",
                null, "English", null, token, Instant.now().plus(7, ChronoUnit.DAYS));
        when(teacherRepository.findByInvitationToken(token)).thenReturn(Optional.of(stub));

        Optional<Teacher> result = service.findByToken(token);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("a@b.com");
    }

    @Test
    @DisplayName("findByToken — expired token returns empty")
    void findByToken_expiredToken_empty() {
        String token = UUID.randomUUID().toString();
        Teacher stub = Teacher.createInvitationStub(CENTER_ID, "A", "B", "a@b.com",
                null, "English", null, token, Instant.now().minus(1, ChronoUnit.HOURS));
        when(teacherRepository.findByInvitationToken(token)).thenReturn(Optional.of(stub));

        assertThat(service.findByToken(token)).isEmpty();
    }

    @Test
    @DisplayName("findByToken — unknown token returns empty")
    void findByToken_unknownToken_empty() {
        when(teacherRepository.findByInvitationToken(anyString())).thenReturn(Optional.empty());

        assertThat(service.findByToken("no-such-token")).isEmpty();
    }

    // ─── acceptInvitation ─────────────────────────────────────────────────────

    @Test
    @DisplayName("acceptInvitation — valid token links userId and transitions stub to ACTIVE")
    void acceptInvitation_validToken_linksUserAndActivates() {
        String token = UUID.randomUUID().toString();
        Teacher stub = Teacher.createInvitationStub(CENTER_ID, "A", "B", "a@b.com",
                null, "English", null, token, Instant.now().plus(7, ChronoUnit.DAYS));
        when(teacherRepository.findByInvitationToken(token)).thenReturn(Optional.of(stub));
        when(teacherRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UUID userId = UUID.randomUUID();
        service.acceptInvitation(token, userId);

        assertThat(stub.getUserId()).isEqualTo(userId);
        assertThat(stub.getStatus()).isEqualTo(TeacherStatus.ACTIVE);
        verify(teacherRepository).save(stub);
    }

    @Test
    @DisplayName("acceptInvitation — unknown token throws IllegalArgumentException")
    void acceptInvitation_unknownToken_throws() {
        when(teacherRepository.findByInvitationToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptInvitation("bad-token", UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired invitation token");
    }

    @Test
    @DisplayName("acceptInvitation — expired token throws IllegalArgumentException")
    void acceptInvitation_expiredToken_throws() {
        String token = UUID.randomUUID().toString();
        Teacher stub = Teacher.createInvitationStub(CENTER_ID, "A", "B", "a@b.com",
                null, "English", null, token, Instant.now().minus(1, ChronoUnit.HOURS));
        when(teacherRepository.findByInvitationToken(token)).thenReturn(Optional.of(stub));

        assertThatThrownBy(() -> service.acceptInvitation(token, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }
}
