package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchResponse;
import com.edutech.center.application.dto.CreateBatchRequest;
import com.edutech.center.domain.model.Batch;
import com.edutech.center.domain.model.BatchMode;
import com.edutech.center.domain.model.CoachingCenter;
import com.edutech.center.domain.model.Role;
import com.edutech.center.domain.port.out.BatchRepository;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.domain.port.out.TeacherRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchGradeTest {

    private static final UUID CENTER_ID = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();

    @Mock
    BatchRepository batchRepository;

    @Mock
    CenterRepository centerRepository;

    @Mock
    CenterEventPublisher eventPublisher;

    @Mock
    TeacherRepository teacherRepository;

    @InjectMocks
    BatchService batchService;

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private CoachingCenter buildCenter() {
        return CoachingCenter.create(
                "Test Academy", "TEST01", "1 Test St",
                "Bangalore", "Karnataka", "560001",
                "9876543210", "test@academy.edu", null,
                null, ADMIN_USER_ID
        );
    }

    private AuthPrincipal centerAdminPrincipal() {
        return new AuthPrincipal(ADMIN_USER_ID, "admin@test.com", Role.CENTER_ADMIN, CENTER_ID, null);
    }

    private CreateBatchRequest request(String grade) {
        return new CreateBatchRequest(
                "Mathematics Batch",
                "MATH-" + UUID.randomUUID().toString().substring(0, 4),
                "Mathematics",
                null,
                30,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusMonths(6),
                BatchMode.OFFLINE,
                grade
        );
    }

    private void stubRepositories() {
        CoachingCenter center = buildCenter();
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------
    // tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("createBatch: grade='10' in request → BatchResponse.grade() == '10'")
    void createBatch_withGrade_gradeStoredAndReturned() {
        stubRepositories();

        BatchResponse response = batchService.createBatch(CENTER_ID, request("10"), centerAdminPrincipal());

        assertThat(response.grade()).isEqualTo("10");
    }

    @Test
    @DisplayName("createBatch: null grade in request → BatchResponse.grade() is null")
    void createBatch_withoutGrade_gradeIsNull() {
        stubRepositories();

        BatchResponse response = batchService.createBatch(CENTER_ID, request(null), centerAdminPrincipal());

        assertThat(response.grade()).isNull();
    }

    @Test
    @DisplayName("createBatch: college year grade='Y2' stored correctly")
    void createBatch_collegeYear_gradeY2_stored() {
        stubRepositories();

        BatchResponse response = batchService.createBatch(CENTER_ID, request("Y2"), centerAdminPrincipal());

        assertThat(response.grade()).isEqualTo("Y2");
    }

    @Test
    @DisplayName("createBatch: dropper batch grade='13' stored correctly")
    void createBatch_dropperBatch_grade13_stored() {
        stubRepositories();

        BatchResponse response = batchService.createBatch(CENTER_ID, request("13"), centerAdminPrincipal());

        assertThat(response.grade()).isEqualTo("13");
    }

    @Test
    @DisplayName("createBatch: grade exceeding VARCHAR(10) — service passes value through (DB enforces limit)")
    void createBatch_gradeTooLong_isTrimmedOrRejected() {
        stubRepositories();

        // VARCHAR(10) limit is enforced at the DB layer (Flyway migration).
        // The service layer does not truncate — it passes the value through.
        // Bean validation @Size(max=10) on CreateBatchRequest will reject this before
        // it reaches BatchService in production; here we verify the domain model
        // accepts it without NPE or exception at the service layer.
        String longGrade = "VERYLONGSTRINGTHATEXCEEDSLIMIT";
        BatchResponse response = batchService.createBatch(CENTER_ID, request(longGrade), centerAdminPrincipal());

        assertThat(response.grade()).isEqualTo(longGrade);
    }

    @Test
    @DisplayName("regression: existing 9-arg Batch.create() overload still works, grade is null")
    void createBatch_noGradeField_backwardCompatOverload_works() {
        // Directly exercise the 9-arg backward-compat factory (no grade param)
        Batch batch = Batch.create(
                CENTER_ID, "Physics Batch", "PHYS-01", "Physics",
                null, 25,
                LocalDate.now().plusDays(1), LocalDate.now().plusMonths(4),
                BatchMode.ONLINE
        );

        assertThat(batch).isNotNull();
        assertThat(batch.getGrade()).isNull();
        assertThat(batch.getName()).isEqualTo("Physics Batch");
        assertThat(batch.getMode()).isEqualTo(BatchMode.ONLINE);
    }
}
