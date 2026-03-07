package com.edutech.mentorsvc.application.service;

import com.edutech.mentorsvc.application.dto.MentorProfileResponse;
import com.edutech.mentorsvc.application.dto.RegisterMentorRequest;
import com.edutech.mentorsvc.application.exception.MentorNotFoundException;
import com.edutech.mentorsvc.application.exception.MentorSvcException;
import com.edutech.mentorsvc.domain.model.MentorProfile;
import com.edutech.mentorsvc.domain.port.out.MentorEventPublisher;
import com.edutech.mentorsvc.domain.port.out.MentorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MentorProfileServiceTest {

    @Mock
    private MentorProfileRepository mentorProfileRepository;

    @Mock
    private MentorEventPublisher mentorEventPublisher;

    private MentorProfileService mentorProfileService;

    @BeforeEach
    void setUp() {
        mentorProfileService = new MentorProfileService(mentorProfileRepository, mentorEventPublisher);
    }

    @Test
    void registerMentor_success() {
        // Given
        RegisterMentorRequest request = new RegisterMentorRequest(
                UUID.randomUUID(),
                "Dr. Arun Kumar",
                "arun.kumar@edutech.com",
                "Expert JEE mentor with 10 years of experience.",
                "JEE,NEET",
                10,
                new BigDecimal("1500.00")
        );

        MentorProfile savedProfile = MentorProfile.create(
                request.userId(),
                request.fullName(),
                request.email(),
                request.bio(),
                request.specializations(),
                request.yearsOfExperience(),
                request.hourlyRate()
        );

        when(mentorProfileRepository.existsByEmail(request.email())).thenReturn(false);
        when(mentorProfileRepository.save(any(MentorProfile.class))).thenReturn(savedProfile);

        // When
        MentorProfileResponse response = mentorProfileService.registerMentor(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.fullName()).isEqualTo(request.fullName());
        assertThat(response.isAvailable()).isTrue();
        verify(mentorProfileRepository, times(1)).save(any(MentorProfile.class));
    }

    @Test
    void registerMentor_duplicateEmail() {
        // Given
        RegisterMentorRequest request = new RegisterMentorRequest(
                UUID.randomUUID(),
                "Priya Sharma",
                "priya.sharma@edutech.com",
                "NEET specialist",
                "NEET",
                5,
                new BigDecimal("1200.00")
        );

        when(mentorProfileRepository.existsByEmail(request.email())).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> mentorProfileService.registerMentor(request))
                .isInstanceOf(MentorSvcException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getMentor_found() {
        // Given
        UUID mentorId = UUID.randomUUID();
        MentorProfile profile = MentorProfile.create(
                UUID.randomUUID(),
                "Ravi Verma",
                "ravi.verma@edutech.com",
                "UPSC mentor",
                "UPSC",
                8,
                new BigDecimal("2000.00")
        );

        when(mentorProfileRepository.findById(mentorId)).thenReturn(Optional.of(profile));

        // When
        MentorProfileResponse response = mentorProfileService.getMentorById(mentorId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("ravi.verma@edutech.com");
        assertThat(response.fullName()).isEqualTo("Ravi Verma");
    }

    @Test
    void getMentor_notFound() {
        // Given
        UUID mentorId = UUID.randomUUID();
        when(mentorProfileRepository.findById(mentorId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> mentorProfileService.getMentorById(mentorId))
                .isInstanceOf(MentorNotFoundException.class)
                .hasMessageContaining(mentorId.toString());
    }

    @Test
    void updateAvailability_success() {
        // Given
        UUID mentorId = UUID.randomUUID();
        MentorProfile profile = MentorProfile.create(
                UUID.randomUUID(),
                "Sneha Patel",
                "sneha.patel@edutech.com",
                "CAT mentor",
                "CAT",
                6,
                new BigDecimal("1800.00")
        );

        when(mentorProfileRepository.findById(mentorId)).thenReturn(Optional.of(profile));
        when(mentorProfileRepository.save(any(MentorProfile.class))).thenReturn(profile);

        // When
        mentorProfileService.updateAvailability(mentorId, false);

        // Then
        verify(mentorProfileRepository, times(1)).save(any(MentorProfile.class));
    }
}
