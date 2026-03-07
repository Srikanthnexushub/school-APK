package com.edutech.careeroracle.application.service;

import com.edutech.careeroracle.application.dto.CareerProfileResponse;
import com.edutech.careeroracle.application.dto.CreateCareerProfileRequest;
import com.edutech.careeroracle.application.dto.UpdateCareerProfileRequest;
import com.edutech.careeroracle.application.exception.CareerOracleException;
import com.edutech.careeroracle.application.exception.CareerProfileNotFoundException;
import com.edutech.careeroracle.domain.event.CareerProfileCreatedEvent;
import com.edutech.careeroracle.domain.model.CareerProfile;
import com.edutech.careeroracle.domain.port.in.CreateCareerProfileUseCase;
import com.edutech.careeroracle.domain.port.in.GetCareerProfileUseCase;
import com.edutech.careeroracle.domain.port.in.UpdateCareerProfileUseCase;
import com.edutech.careeroracle.domain.port.out.CareerOracleEventPublisher;
import com.edutech.careeroracle.domain.port.out.CareerProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class CareerProfileService implements CreateCareerProfileUseCase, GetCareerProfileUseCase, UpdateCareerProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(CareerProfileService.class);

    private final CareerProfileRepository careerProfileRepository;
    private final CareerOracleEventPublisher eventPublisher;

    public CareerProfileService(CareerProfileRepository careerProfileRepository,
                                 CareerOracleEventPublisher eventPublisher) {
        this.careerProfileRepository = careerProfileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public CareerProfileResponse createCareerProfile(CreateCareerProfileRequest request) {
        if (careerProfileRepository.existsByStudentId(request.studentId())) {
            throw new CareerOracleException("Career profile already exists for student: " + request.studentId());
        }

        CareerProfile profile = CareerProfile.create(
                request.studentId(),
                request.enrollmentId(),
                request.academicStream(),
                request.currentGrade(),
                request.ersScore(),
                request.preferredCareerStream()
        );

        CareerProfile saved = careerProfileRepository.save(profile);

        CareerProfileCreatedEvent event = new CareerProfileCreatedEvent(
                saved.getId(),
                saved.getStudentId(),
                saved.getEnrollmentId(),
                saved.getAcademicStream(),
                saved.getCurrentGrade(),
                OffsetDateTime.now()
        );

        try {
            eventPublisher.publishCareerProfileCreated(event);
        } catch (Exception ex) {
            log.warn("Failed to publish CareerProfileCreatedEvent for profileId={}: {}", saved.getId(), ex.getMessage());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CareerProfileResponse getCareerProfileByStudentId(UUID studentId) {
        CareerProfile profile = careerProfileRepository.findByStudentId(studentId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new CareerProfileNotFoundException("studentId", studentId));
        return toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public CareerProfileResponse getCareerProfileById(UUID profileId) {
        CareerProfile profile = careerProfileRepository.findById(profileId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new CareerProfileNotFoundException(profileId));
        return toResponse(profile);
    }

    @Override
    @Transactional
    public CareerProfileResponse updateCareerProfile(UUID profileId, UpdateCareerProfileRequest request) {
        CareerProfile profile = careerProfileRepository.findById(profileId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new CareerProfileNotFoundException(profileId));

        profile.update(
                request.academicStream(),
                request.currentGrade(),
                request.ersScore(),
                request.preferredCareerStream()
        );

        CareerProfile saved = careerProfileRepository.save(profile);
        return toResponse(saved);
    }

    private CareerProfileResponse toResponse(CareerProfile profile) {
        return new CareerProfileResponse(
                profile.getId(),
                profile.getStudentId(),
                profile.getEnrollmentId(),
                profile.getAcademicStream(),
                profile.getCurrentGrade(),
                profile.getErsScore(),
                profile.getPreferredCareerStream(),
                profile.getVersion(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
