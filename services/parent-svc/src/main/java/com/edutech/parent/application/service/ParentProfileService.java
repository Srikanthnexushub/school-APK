// src/main/java/com/edutech/parent/application/service/ParentProfileService.java
package com.edutech.parent.application.service;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.CreateParentProfileRequest;
import com.edutech.parent.application.dto.ParentProfileResponse;
import com.edutech.parent.application.dto.UpdateParentProfileRequest;
import com.edutech.parent.application.exception.ParentAccessDeniedException;
import com.edutech.parent.application.exception.ParentProfileNotFoundException;
import com.edutech.parent.domain.model.ParentProfile;
import com.edutech.parent.domain.port.in.CreateParentProfileUseCase;
import com.edutech.parent.domain.port.in.UpdateParentProfileUseCase;
import com.edutech.parent.domain.port.out.ParentEventPublisher;
import com.edutech.parent.domain.port.out.ParentProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ParentProfileService implements CreateParentProfileUseCase, UpdateParentProfileUseCase {

    private static final Logger log = LoggerFactory.getLogger(ParentProfileService.class);

    private final ParentProfileRepository profileRepository;
    private final ParentEventPublisher eventPublisher;

    public ParentProfileService(ParentProfileRepository profileRepository,
                                 ParentEventPublisher eventPublisher) {
        this.profileRepository = profileRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ParentProfileResponse createProfile(CreateParentProfileRequest request, AuthPrincipal principal) {
        UUID ownerId = principal.userId();
        ParentProfile profile = ParentProfile.create(
                ownerId,
                request.name(),
                request.phone(),
                request.email(),
                request.address(),
                request.city(),
                request.state(),
                request.pincode(),
                request.relationshipType()
        );
        ParentProfile saved = profileRepository.save(profile);
        log.info("Parent profile created: id={} userId={}", saved.getId(), ownerId);
        return toResponse(saved);
    }

    @Override
    public ParentProfileResponse updateProfile(UUID profileId, UpdateParentProfileRequest request, AuthPrincipal principal) {
        ParentProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ParentProfileNotFoundException(profileId));
        if (!principal.ownsProfile(profile.getUserId())) {
            throw new ParentAccessDeniedException();
        }
        profile.update(
                request.name(),
                request.phone(),
                request.email(),
                request.address(),
                request.city(),
                request.state(),
                request.pincode(),
                request.relationshipType()
        );
        return toResponse(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public ParentProfileResponse getProfile(UUID profileId, AuthPrincipal principal) {
        ParentProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ParentProfileNotFoundException(profileId));
        if (!principal.ownsProfile(profile.getUserId())) {
            throw new ParentAccessDeniedException();
        }
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public ParentProfileResponse getMyProfile(AuthPrincipal principal) {
        return profileRepository.findByUserId(principal.userId())
                .map(this::toResponse)
                .orElseThrow(() -> new ParentProfileNotFoundException(null));
    }

    private ParentProfileResponse toResponse(ParentProfile p) {
        return new ParentProfileResponse(
                p.getId(),
                p.getUserId(),
                p.getName(),
                p.getPhone(),
                p.getEmail(),
                p.getAddress(),
                p.getCity(),
                p.getState(),
                p.getPincode(),
                p.getRelationshipType(),
                p.isVerified(),
                p.getStatus(),
                p.getCreatedAt()
        );
    }
}
