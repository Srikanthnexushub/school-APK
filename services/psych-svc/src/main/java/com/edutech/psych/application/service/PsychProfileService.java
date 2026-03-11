package com.edutech.psych.application.service;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.application.dto.CreatePsychProfileRequest;
import com.edutech.psych.application.dto.PsychProfileResponse;
import com.edutech.psych.application.exception.PsychAccessDeniedException;
import com.edutech.psych.application.exception.PsychProfileNotFoundException;
import com.edutech.psych.domain.event.PsychProfileCreatedEvent;
import com.edutech.psych.domain.model.PsychProfile;
import com.edutech.psych.domain.port.in.CreatePsychProfileUseCase;
import com.edutech.psych.domain.port.out.PsychEventPublisher;
import com.edutech.psych.domain.port.out.PsychProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PsychProfileService implements CreatePsychProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(PsychProfileService.class);

    private final PsychProfileRepository profileRepository;
    private final PsychEventPublisher eventPublisher;

    public PsychProfileService(PsychProfileRepository profileRepository,
                               PsychEventPublisher eventPublisher) {
        this.profileRepository = profileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PsychProfileResponse createProfile(CreatePsychProfileRequest req, AuthPrincipal principal) {
        if (!principal.belongsToCenter(req.centerId())) {
            throw new PsychAccessDeniedException();
        }

        PsychProfile profile = PsychProfile.create(req.studentId(), req.centerId(), req.batchId());
        profile.activate();
        PsychProfile saved = profileRepository.save(profile);

        eventPublisher.publish(new PsychProfileCreatedEvent(
                saved.getId(),
                saved.getStudentId(),
                saved.getCenterId(),
                saved.getBatchId()
        ));

        log.info("Created and activated psych profile id={} for studentId={} centerId={}",
                saved.getId(), saved.getStudentId(), saved.getCenterId());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Optional<PsychProfileResponse> getProfile(UUID profileId, AuthPrincipal principal) {
        return profileRepository.findById(profileId)
                .map(profile -> {
                    if (!principal.belongsToCenter(profile.getCenterId())) {
                        throw new PsychAccessDeniedException();
                    }
                    return toResponse(profile);
                });
    }

    @Transactional(readOnly = true)
    public List<PsychProfileResponse> listByStudentId(UUID studentId, AuthPrincipal principal) {
        if (!principal.isSuperAdmin() && !principal.isParent() && !principal.userId().equals(studentId)) {
            throw new PsychAccessDeniedException();
        }
        return profileRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PsychProfileResponse> listByCenterId(UUID centerId, AuthPrincipal principal) {
        if (!principal.belongsToCenter(centerId)) {
            throw new PsychAccessDeniedException();
        }
        return profileRepository.findByCenterId(centerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PsychProfileResponse toResponse(PsychProfile p) {
        return new PsychProfileResponse(
                p.getId(),
                p.getStudentId(),
                p.getCenterId(),
                p.getBatchId(),
                p.getOpenness(),
                p.getConscientiousness(),
                p.getExtraversion(),
                p.getAgreeableness(),
                p.getNeuroticism(),
                p.getRiasecCode(),
                p.getStatus(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
