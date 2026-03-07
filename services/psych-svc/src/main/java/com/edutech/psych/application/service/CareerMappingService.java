package com.edutech.psych.application.service;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.application.dto.CareerMappingResponse;
import com.edutech.psych.application.exception.PsychAccessDeniedException;
import com.edutech.psych.application.exception.PsychProfileNotFoundException;
import com.edutech.psych.application.exception.ProfileNotActiveException;
import com.edutech.psych.domain.event.CareerMappingGeneratedEvent;
import com.edutech.psych.domain.model.CareerMapping;
import com.edutech.psych.domain.model.ProfileStatus;
import com.edutech.psych.domain.model.PsychProfile;
import com.edutech.psych.domain.port.in.RequestCareerMappingUseCase;
import com.edutech.psych.domain.port.out.CareerMappingRepository;
import com.edutech.psych.domain.port.out.CareerPredictionResponse;
import com.edutech.psych.domain.port.out.PsychAiSvcClient;
import com.edutech.psych.domain.port.out.PsychEventPublisher;
import com.edutech.psych.domain.port.out.PsychProfileRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CareerMappingService implements RequestCareerMappingUseCase {

    private static final Logger log = LoggerFactory.getLogger(CareerMappingService.class);

    private final PsychProfileRepository profileRepository;
    private final CareerMappingRepository careerMappingRepository;
    private final PsychAiSvcClient psychAiSvcClient;
    private final PsychEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public CareerMappingService(PsychProfileRepository profileRepository,
                                CareerMappingRepository careerMappingRepository,
                                PsychAiSvcClient psychAiSvcClient,
                                PsychEventPublisher eventPublisher,
                                ObjectMapper objectMapper) {
        this.profileRepository = profileRepository;
        this.careerMappingRepository = careerMappingRepository;
        this.psychAiSvcClient = psychAiSvcClient;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public CareerMappingResponse requestCareerMapping(UUID profileId, AuthPrincipal principal) {
        PsychProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new PsychProfileNotFoundException(profileId));

        if (!principal.userId().equals(profile.getStudentId()) && !principal.isSuperAdmin()) {
            throw new PsychAccessDeniedException();
        }

        if (profile.getStatus() != ProfileStatus.ACTIVE) {
            throw new ProfileNotActiveException(profileId);
        }

        CareerMapping mapping = CareerMapping.create(
                profileId,
                profile.getStudentId(),
                profile.getCenterId()
        );
        mapping = careerMappingRepository.save(mapping);

        try {
            CareerPredictionResponse prediction = psychAiSvcClient.predictCareers(
                    profile.getId(),
                    profile.getOpenness(),
                    profile.getConscientiousness(),
                    profile.getExtraversion(),
                    profile.getAgreeableness(),
                    profile.getNeuroticism(),
                    profile.getRiasecCode()
            );

            String topCareersJson = serializeTopCareers(prediction.topCareers());

            mapping.complete(topCareersJson, prediction.reasoning(), prediction.modelVersion());
            mapping = careerMappingRepository.save(mapping);

            eventPublisher.publish(new CareerMappingGeneratedEvent(
                    mapping.getId(),
                    mapping.getProfileId(),
                    mapping.getStudentId(),
                    mapping.getCenterId(),
                    topCareersJson
            ));

            log.info("Career mapping generated id={} for profileId={} studentId={}",
                    mapping.getId(), profileId, profile.getStudentId());

        } catch (Exception e) {
            log.error("Career mapping failed for profileId={}: {}", profileId, e.getMessage());
            mapping.fail();
            careerMappingRepository.save(mapping);
        }

        return toResponse(mapping);
    }

    @Transactional(readOnly = true)
    public List<CareerMappingResponse> getCareerMappings(UUID profileId, AuthPrincipal principal) {
        PsychProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new PsychProfileNotFoundException(profileId));

        if (!principal.userId().equals(profile.getStudentId()) && !principal.isSuperAdmin()) {
            throw new PsychAccessDeniedException();
        }

        return careerMappingRepository.findByProfileId(profileId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private String serializeTopCareers(List<String> topCareers) {
        try {
            return objectMapper.writeValueAsString(topCareers);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize topCareers list, falling back to toString: {}", e.getMessage());
            return topCareers.toString();
        }
    }

    private CareerMappingResponse toResponse(CareerMapping m) {
        return new CareerMappingResponse(
                m.getId(),
                m.getProfileId(),
                m.getStudentId(),
                m.getStatus(),
                m.getRequestedAt(),
                m.getGeneratedAt(),
                m.getTopCareers(),
                m.getReasoning(),
                m.getModelVersion(),
                m.getCreatedAt()
        );
    }
}
