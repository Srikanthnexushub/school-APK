package com.edutech.center.application.service;

import com.edutech.center.application.dto.CenterLookupResponse;
import com.edutech.center.domain.model.CoachingCenter;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.domain.port.out.TeacherRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CenterLookupTest {

    @Mock
    CenterRepository centerRepository;

    @Mock
    CenterEventPublisher eventPublisher;

    @Mock
    TeacherRepository teacherRepository;

    @InjectMocks
    CenterService centerService;

    @Test
    @DisplayName("lookupByCode: existing code returns center details")
    void lookupByCode_existingCode_returnsCenterDetails() {
        CoachingCenter center = CoachingCenter.create(
            "Sunrise Academy", "SUNR01", "123 Main St",
            "Bangalore", "Karnataka", "560001",
            "9876543210", "info@sunrise.edu", null,
            null, UUID.randomUUID()
        );
        UUID expectedId = center.getId();

        when(centerRepository.findByCode("SUNR01")).thenReturn(Optional.of(center));

        Optional<CenterLookupResponse> result = centerService.lookupByCode("SUNR01");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(expectedId);
        assertThat(result.get().name()).isEqualTo("Sunrise Academy");
        assertThat(result.get().city()).isEqualTo("Bangalore");
    }

    @Test
    @DisplayName("lookupByCode: non-existent code returns empty")
    void lookupByCode_nonExistentCode_returnsEmpty() {
        when(centerRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        Optional<CenterLookupResponse> result = centerService.lookupByCode("UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("lookupByCode: deleted center returns empty (excluded by repository query)")
    void lookupByCode_deletedCenter_returnsEmpty() {
        // The findByCodeActive JPQL query filters deletedAt IS NULL at the DB level,
        // so the repository returns Optional.empty() for soft-deleted centers.
        when(centerRepository.findByCode("DELETED01")).thenReturn(Optional.empty());

        Optional<CenterLookupResponse> result = centerService.lookupByCode("DELETED01");

        assertThat(result).isEmpty();
    }
}
