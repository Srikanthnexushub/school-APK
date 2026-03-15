package com.edutech.mentorsvc.application.service;

import com.edutech.mentorsvc.application.dto.MentorProfileResponse;
import com.edutech.mentorsvc.application.dto.RegisterMentorRequest;
import com.edutech.mentorsvc.application.dto.UpdateMentorProfileRequest;
import com.edutech.mentorsvc.application.exception.MentorNotFoundException;
import com.edutech.mentorsvc.application.exception.MentorSvcException;
import com.edutech.mentorsvc.domain.model.MentorProfile;
import com.edutech.mentorsvc.domain.port.in.GetMentorProfileUseCase;
import com.edutech.mentorsvc.domain.port.in.RegisterMentorUseCase;
import com.edutech.mentorsvc.domain.port.in.UpdateMentorAvailabilityUseCase;
import com.edutech.mentorsvc.domain.port.in.UpdateMentorProfileUseCase;
import com.edutech.mentorsvc.domain.port.out.MentorEventPublisher;
import com.edutech.mentorsvc.domain.port.out.MentorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MentorProfileService implements RegisterMentorUseCase, GetMentorProfileUseCase,
        UpdateMentorAvailabilityUseCase, UpdateMentorProfileUseCase {

    private final MentorProfileRepository mentorProfileRepository;
    private final MentorEventPublisher mentorEventPublisher;

    public MentorProfileService(MentorProfileRepository mentorProfileRepository,
                                MentorEventPublisher mentorEventPublisher) {
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorEventPublisher = mentorEventPublisher;
    }

    @Override
    public MentorProfileResponse registerMentor(RegisterMentorRequest request) {
        if (mentorProfileRepository.existsByEmail(request.email())) {
            throw new MentorSvcException(
                    "A mentor with email '" + request.email() + "' already exists.",
                    "DUPLICATE_MENTOR_EMAIL"
            );
        }
        MentorProfile profile = MentorProfile.create(
                request.userId(),
                request.fullName(),
                request.email(),
                request.bio(),
                request.specializations(),
                request.yearsOfExperience(),
                request.hourlyRate()
        );
        MentorProfile saved = mentorProfileRepository.save(profile);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorProfileResponse getMentorById(UUID mentorId) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new MentorNotFoundException(mentorId));
        return toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorProfileResponse getMentorByUserId(UUID userId) {
        MentorProfile profile = mentorProfileRepository.findByUserId(userId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new MentorNotFoundException(userId));
        return toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorProfileResponse> getAllAvailableMentors() {
        return mentorProfileRepository.findAllAvailable()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MentorProfileResponse updateProfile(UUID userId, UpdateMentorProfileRequest request) {
        MentorProfile profile = mentorProfileRepository.findByUserId(userId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new MentorNotFoundException(userId));
        profile.update(request.fullName(), request.bio(), request.specializations(),
                request.yearsOfExperience(), request.hourlyRate(), request.gender(), request.district());
        MentorProfile saved = mentorProfileRepository.save(profile);
        return toResponse(saved);
    }

    @Override
    public void updateAvailability(UUID mentorId, boolean isAvailable) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new MentorNotFoundException(mentorId));
        profile.updateAvailability(isAvailable);
        mentorProfileRepository.save(profile);
    }

    private MentorProfileResponse toResponse(MentorProfile profile) {
        return new MentorProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getFullName(),
                profile.getEmail(),
                profile.getBio(),
                profile.getSpecializations(),
                profile.getYearsOfExperience(),
                profile.getHourlyRate(),
                profile.isAvailable(),
                profile.getAverageRating(),
                profile.getTotalSessions(),
                profile.getGender(),
                profile.getDistrict(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
