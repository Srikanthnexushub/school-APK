// src/test/java/com/edutech/center/application/service/BatchServiceTest.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.BatchResponse;
import com.edutech.center.application.dto.CreateBatchRequest;
import com.edutech.center.application.dto.UpdateBatchRequest;
import com.edutech.center.application.exception.BatchNotFoundException;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterNotFoundException;
import com.edutech.center.domain.model.Batch;
import com.edutech.center.domain.model.BatchStatus;
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

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchService unit tests")
class BatchServiceTest {

    @Mock BatchRepository batchRepository;
    @Mock CenterRepository centerRepository;
    @Mock CenterEventPublisher eventPublisher;
    @Mock TeacherRepository teacherRepository;
    @InjectMocks BatchService batchService;

    private static final UUID CENTER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private AuthPrincipal centerAdminPrincipal() {
        return new AuthPrincipal(USER_ID, "admin@center.com", Role.CENTER_ADMIN, CENTER_ID, "fp");
    }

    private AuthPrincipal otherCenterPrincipal() {
        return new AuthPrincipal(UUID.randomUUID(), "other@x.com", Role.CENTER_ADMIN, UUID.randomUUID(), "fp");
    }

    private CreateBatchRequest validRequest() {
        return new CreateBatchRequest("Maths Batch A", "MBA001", "Mathematics",
                null, 30, LocalDate.now(), LocalDate.now().plusMonths(6));
    }

    @Test
    @DisplayName("Create batch — success")
    void createBatch_success() {
        CoachingCenter center = mock(CoachingCenter.class);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(batchRepository.save(any(Batch.class))).thenAnswer(i -> i.getArgument(0));

        BatchResponse response = batchService.createBatch(CENTER_ID, validRequest(), centerAdminPrincipal());

        assertThat(response).isNotNull();
        assertThat(response.centerId()).isEqualTo(CENTER_ID);
        assertThat(response.subject()).isEqualTo("Mathematics");
        assertThat(response.status()).isEqualTo(BatchStatus.UPCOMING);
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Create batch — center not found throws CenterNotFoundException")
    void createBatch_centerNotFound() {
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.createBatch(CENTER_ID, validRequest(), centerAdminPrincipal()))
            .isInstanceOf(CenterNotFoundException.class);
    }

    @Test
    @DisplayName("Create batch — wrong center throws CenterAccessDeniedException")
    void createBatch_wrongCenter() {
        assertThatThrownBy(() -> batchService.createBatch(CENTER_ID, validRequest(), otherCenterPrincipal()))
            .isInstanceOf(CenterAccessDeniedException.class);
    }

    @Test
    @DisplayName("Update batch — activate transitions from UPCOMING to ACTIVE")
    void updateBatch_activate() {
        Batch batch = Batch.create(CENTER_ID, "Batch", "B001", "Science",
                null, 20, LocalDate.now(), null);

        when(batchRepository.findById(batch.getId())).thenReturn(Optional.of(batch));
        when(batchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateBatchRequest req = new UpdateBatchRequest(null, BatchStatus.ACTIVE);
        BatchResponse response = batchService.updateBatch(batch.getId(), req, centerAdminPrincipal());

        assertThat(response.status()).isEqualTo(BatchStatus.ACTIVE);
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Update batch — batch not found throws BatchNotFoundException")
    void updateBatch_notFound() {
        UUID batchId = UUID.randomUUID();
        when(batchRepository.findById(batchId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.updateBatch(batchId,
                new UpdateBatchRequest(null, BatchStatus.ACTIVE), centerAdminPrincipal()))
            .isInstanceOf(BatchNotFoundException.class);
    }

    @Test
    @DisplayName("SUPER_ADMIN can create batch in any center")
    void createBatch_superAdminCanAccessAnyCenter() {
        AuthPrincipal superAdmin = new AuthPrincipal(UUID.randomUUID(), "super@x.com",
                Role.SUPER_ADMIN, null, "fp");
        CoachingCenter center = mock(CoachingCenter.class);
        when(centerRepository.findById(CENTER_ID)).thenReturn(Optional.of(center));
        when(batchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BatchResponse response = batchService.createBatch(CENTER_ID, validRequest(), superAdmin);
        assertThat(response).isNotNull();
    }
}
