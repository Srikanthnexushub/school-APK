package com.edutech.psych.application.service;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.application.dto.CompleteSessionRequest;
import com.edutech.psych.application.dto.SessionResponse;
import com.edutech.psych.application.dto.StartSessionRequest;
import com.edutech.psych.application.exception.PsychAccessDeniedException;
import com.edutech.psych.application.exception.PsychProfileNotFoundException;
import com.edutech.psych.application.exception.ProfileNotActiveException;
import com.edutech.psych.application.exception.SessionAlreadyCompletedException;
import com.edutech.psych.application.exception.SessionHistoryNotFoundException;
import com.edutech.psych.domain.event.SessionCompletedEvent;
import com.edutech.psych.domain.model.ProfileStatus;
import com.edutech.psych.domain.model.PsychProfile;
import com.edutech.psych.domain.model.SessionHistory;
import com.edutech.psych.domain.model.SessionStatus;
import com.edutech.psych.domain.port.in.CompleteSessionUseCase;
import com.edutech.psych.domain.port.in.StartSessionUseCase;
import com.edutech.psych.domain.port.out.PsychEventPublisher;
import com.edutech.psych.domain.port.out.PsychProfileRepository;
import com.edutech.psych.domain.port.out.SessionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Service
public class SessionService implements StartSessionUseCase, CompleteSessionUseCase {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final PsychProfileRepository profileRepository;
    private final SessionHistoryRepository sessionHistoryRepository;
    private final PsychEventPublisher eventPublisher;

    public SessionService(PsychProfileRepository profileRepository,
                          SessionHistoryRepository sessionHistoryRepository,
                          PsychEventPublisher eventPublisher) {
        this.profileRepository = profileRepository;
        this.sessionHistoryRepository = sessionHistoryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public SessionResponse startSession(UUID profileId, StartSessionRequest req, AuthPrincipal principal) {
        PsychProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new PsychProfileNotFoundException(profileId));

        if (!principal.userId().equals(profile.getStudentId()) && !principal.isSuperAdmin()) {
            throw new PsychAccessDeniedException();
        }

        if (profile.getStatus() != ProfileStatus.ACTIVE) {
            throw new ProfileNotActiveException(profileId);
        }

        SessionHistory session = SessionHistory.create(
                profileId,
                profile.getStudentId(),
                profile.getCenterId(),
                req.sessionType(),
                req.scheduledAt()
        );
        session.start();
        SessionHistory saved = sessionHistoryRepository.save(session);

        log.info("Started session id={} for profileId={} studentId={}",
                saved.getId(), profileId, profile.getStudentId());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public SessionResponse completeSession(UUID sessionId, CompleteSessionRequest req, AuthPrincipal principal) {
        SessionHistory session = sessionHistoryRepository.findById(sessionId)
                .orElseThrow(() -> new SessionHistoryNotFoundException(sessionId));

        if (!principal.userId().equals(session.getStudentId()) && !principal.isSuperAdmin()) {
            throw new PsychAccessDeniedException();
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new SessionAlreadyCompletedException(sessionId);
        }

        session.complete(req.notes());
        sessionHistoryRepository.save(session);

        UUID profileId = session.getProfileId();
        PsychProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new PsychProfileNotFoundException(profileId));

        profile.updateTraits(
                req.openness(),
                req.conscientiousness(),
                req.extraversion(),
                req.agreeableness(),
                req.neuroticism(),
                req.riasecCode()
        );
        profileRepository.save(profile);

        eventPublisher.publish(new SessionCompletedEvent(
                session.getId(),
                session.getProfileId(),
                session.getStudentId(),
                session.getCenterId()
        ));

        log.info("Completed session id={} for profileId={} studentId={}",
                session.getId(), profileId, session.getStudentId());

        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(UUID profileId, AuthPrincipal principal) {
        PsychProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new PsychProfileNotFoundException(profileId));

        if (!principal.userId().equals(profile.getStudentId()) && !principal.isSuperAdmin()) {
            throw new PsychAccessDeniedException();
        }

        return sessionHistoryRepository.findByProfileId(profileId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SessionResponse> listSessions(UUID profileId, AuthPrincipal principal, Pageable pageable) {
        List<SessionResponse> all = listSessions(profileId, principal);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
    }

    private SessionResponse toResponse(SessionHistory s) {
        return new SessionResponse(
                s.getId(),
                s.getProfileId(),
                s.getStudentId(),
                s.getSessionType(),
                s.getStatus(),
                s.getScheduledAt(),
                s.getStartedAt(),
                s.getCompletedAt(),
                s.getNotes(),
                s.getCreatedAt()
        );
    }
}
