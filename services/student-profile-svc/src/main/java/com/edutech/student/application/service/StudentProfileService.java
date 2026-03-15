package com.edutech.student.application.service;

import com.edutech.student.application.dto.CreateStudentProfileRequest;
import com.edutech.student.application.dto.GenerateLinkOtpRequest;
import com.edutech.student.application.dto.GenerateLinkOtpResponse;
import com.edutech.student.application.dto.PendingLinkResponse;
import com.edutech.student.application.dto.StudentLookupResponse;
import com.edutech.student.application.dto.StudentProfileResponse;
import com.edutech.student.application.dto.UpdateStudentProfileRequest;
import com.edutech.student.application.dto.VerifyLinkOtpRequest;
import com.edutech.student.application.dto.VerifyLinkOtpResponse;
import com.edutech.student.application.exception.DuplicateStudentException;
import com.edutech.student.application.exception.InvalidLinkOtpException;
import com.edutech.student.application.exception.StudentNotFoundException;
import com.edutech.student.domain.event.StudentProfileCreatedEvent;
import com.edutech.student.domain.model.StudentProfile;
import com.edutech.student.domain.port.in.CreateStudentProfileUseCase;
import com.edutech.student.domain.port.in.GetStudentProfileUseCase;
import com.edutech.student.domain.port.in.UpdateStudentProfileUseCase;
import com.edutech.student.domain.port.out.StudentEventPublisher;
import com.edutech.student.domain.port.out.StudentProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StudentProfileService implements CreateStudentProfileUseCase,
        GetStudentProfileUseCase,
        UpdateStudentProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(StudentProfileService.class);

    private final StudentProfileRepository profileRepository;
    private final StudentEventPublisher eventPublisher;

    public StudentProfileService(StudentProfileRepository profileRepository,
                                  StudentEventPublisher eventPublisher) {
        this.profileRepository = profileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public StudentProfileResponse createProfile(CreateStudentProfileRequest request) {
        profileRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new DuplicateStudentException(request.email());
        });

        StudentProfile profile = StudentProfile.create(
                request.userId(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.gender(),
                request.dateOfBirth(),
                request.city(),
                request.state(),
                request.pincode(),
                null,
                null,
                request.institutionName(),
                request.board(),
                request.currentClass()
        );

        profile.setSubjects(request.subjects() != null ? request.subjects() : List.of());

        StudentProfile saved = profileRepository.save(profile);
        log.info("Student profile created: id={} userId={}", saved.getId(), saved.getUserId());

        eventPublisher.publish(new StudentProfileCreatedEvent(
                UUID.randomUUID().toString(),
                saved.getId(),
                saved.getUserId(),
                saved.getEmail(),
                saved.getCity(),
                saved.getState(),
                saved.getStream(),
                Instant.now()
        ));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(UUID studentId) {
        return profileRepository.findById(studentId)
                .map(this::toResponse)
                .orElseThrow(() -> new StudentNotFoundException(studentId));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfileResponse getProfileByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new StudentNotFoundException(userId));
    }

    @Override
    public StudentProfileResponse updateProfile(UUID studentId, UpdateStudentProfileRequest request) {
        StudentProfile profile = profileRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException(studentId));

        profile.updateName(request.firstName(), request.lastName());
        profile.updatePhone(request.phone());
        profile.updateGender(request.gender());
        profile.updateLocation(request.city(), request.state(), request.district(), request.country());

        if (request.stream() != null) {
            profile.selectStream(request.stream());
        }
        if (request.targetYear() != null) {
            profile.setTargetYear(request.targetYear());
        }

        StudentProfile saved = profileRepository.save(profile);
        log.info("Student profile updated: id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getByLinkCode(String code) {
        return profileRepository.findByParentLinkCode(code)
                .map(this::toResponse)
                .orElseThrow(() -> new StudentNotFoundException(null));
    }

    public StudentProfileResponse regenerateLinkCode(UUID userId) {
        StudentProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new StudentNotFoundException(userId));
        profile.regenerateLinkCode();
        return toResponse(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public StudentLookupResponse lookupByEmail(String email) {
        StudentProfile p = profileRepository.findByEmail(email)
                .orElseThrow(() -> new StudentNotFoundException(null));
        return new StudentLookupResponse(p.getId(), p.getFirstName(), p.getLastName(),
                p.getEmail(), p.getCity(), p.getCurrentBoard() != null ? p.getCurrentBoard().name() : null, p.getCurrentClass());
    }

    public GenerateLinkOtpResponse generateLinkOtp(GenerateLinkOtpRequest request, UUID parentUserId) {
        StudentProfile profile = profileRepository.findByEmail(request.studentEmail())
                .orElseThrow(() -> new StudentNotFoundException(null));
        profile.generateLinkOtp(parentUserId, request.parentName());
        StudentProfile saved = profileRepository.save(profile);
        log.info("Link OTP generated for student={} by parent={}", saved.getId(), parentUserId);
        return new GenerateLinkOtpResponse(saved.getId(),
                saved.getFirstName() + " " + saved.getLastName(),
                saved.getLinkOtpExpiresAt());
    }

    @Transactional(readOnly = true)
    public PendingLinkResponse getPendingLink(UUID userId) {
        StudentProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new StudentNotFoundException(userId));
        if (!profile.isLinkOtpValid()) {
            return null;
        }
        return new PendingLinkResponse(profile.getLinkOtp(), profile.getLinkOtpParentName(), profile.getLinkOtpExpiresAt());
    }

    public VerifyLinkOtpResponse verifyLinkOtp(VerifyLinkOtpRequest request) {
        StudentProfile profile = profileRepository.findByEmail(request.studentEmail())
                .orElseThrow(() -> new StudentNotFoundException(null));
        if (!profile.isLinkOtpValid() || !profile.getLinkOtp().equals(request.otp())) {
            throw new InvalidLinkOtpException();
        }
        profile.clearLinkOtp();
        profileRepository.save(profile);
        return new VerifyLinkOtpResponse(profile.getId(), profile.getFirstName() + " " + profile.getLastName());
    }

    private StudentProfileResponse toResponse(StudentProfile p) {
        return new StudentProfileResponse(
                p.getId(),
                p.getUserId(),
                p.getFirstName(),
                p.getLastName(),
                p.getEmail(),
                p.getPhone(),
                p.getGender(),
                p.getDateOfBirth(),
                p.getCity(),
                p.getState(),
                p.getCurrentBoard(),
                p.getCurrentClass(),
                p.getStream(),
                p.getTargetYear(),
                p.getStatus(),
                p.getCreatedAt(),
                p.getSubjects(),
                p.getParentLinkCode()
        );
    }
}
