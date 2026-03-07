// src/main/java/com/edutech/parent/application/service/NotificationPreferenceService.java
package com.edutech.parent.application.service;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.CreateNotificationPreferenceRequest;
import com.edutech.parent.application.dto.NotificationPreferenceResponse;
import com.edutech.parent.application.dto.UpdateNotificationPreferenceRequest;
import com.edutech.parent.application.exception.ParentAccessDeniedException;
import com.edutech.parent.application.exception.ParentProfileNotFoundException;
import com.edutech.parent.domain.model.NotificationPreference;
import com.edutech.parent.domain.model.ParentProfile;
import com.edutech.parent.domain.port.out.NotificationPreferenceRepository;
import com.edutech.parent.domain.port.out.ParentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository prefRepository;
    private final ParentProfileRepository profileRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository prefRepository,
                                          ParentProfileRepository profileRepository) {
        this.prefRepository = prefRepository;
        this.profileRepository = profileRepository;
    }

    public NotificationPreferenceResponse upsertPreference(UUID parentProfileId,
                                                            CreateNotificationPreferenceRequest request,
                                                            AuthPrincipal principal) {
        ParentProfile parent = profileRepository.findById(parentProfileId)
                .orElseThrow(() -> new ParentProfileNotFoundException(parentProfileId));
        if (!principal.ownsProfile(parent.getUserId())) {
            throw new ParentAccessDeniedException();
        }
        Optional<NotificationPreference> existing = prefRepository.findByParentIdAndChannelAndEventType(
                parentProfileId, request.channel(), request.eventType());
        if (existing.isPresent()) {
            existing.get().toggle(request.enabled());
            return toResponse(prefRepository.save(existing.get()));
        }
        NotificationPreference pref = NotificationPreference.create(
                parentProfileId, request.channel(), request.eventType(), request.enabled());
        return toResponse(prefRepository.save(pref));
    }

    public NotificationPreferenceResponse updatePreference(UUID parentProfileId,
                                                            UUID prefId,
                                                            UpdateNotificationPreferenceRequest request,
                                                            AuthPrincipal principal) {
        ParentProfile parent = profileRepository.findById(parentProfileId)
                .orElseThrow(() -> new ParentProfileNotFoundException(parentProfileId));
        if (!principal.ownsProfile(parent.getUserId())) {
            throw new ParentAccessDeniedException();
        }
        NotificationPreference pref = prefRepository.findById(prefId)
                .orElseThrow(() -> new RuntimeException("Preference not found: " + prefId));
        pref.toggle(request.enabled());
        return toResponse(prefRepository.save(pref));
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getPreferences(UUID parentProfileId, AuthPrincipal principal) {
        ParentProfile parent = profileRepository.findById(parentProfileId)
                .orElseThrow(() -> new ParentProfileNotFoundException(parentProfileId));
        if (!principal.ownsProfile(parent.getUserId())) {
            throw new ParentAccessDeniedException();
        }
        return prefRepository.findByParentId(parentProfileId).stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationPreferenceResponse toResponse(NotificationPreference p) {
        return new NotificationPreferenceResponse(
                p.getId(),
                p.getParentId(),
                p.getChannel(),
                p.getEventType(),
                p.isEnabled(),
                p.getCreatedAt()
        );
    }
}
