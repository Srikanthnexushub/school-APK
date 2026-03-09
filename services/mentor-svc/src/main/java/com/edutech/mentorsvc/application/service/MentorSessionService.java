package com.edutech.mentorsvc.application.service;

import com.edutech.mentorsvc.application.dto.BookSessionRequest;
import com.edutech.mentorsvc.application.dto.MentorSessionResponse;
import com.edutech.mentorsvc.application.exception.MentorNotFoundException;
import com.edutech.mentorsvc.application.exception.MentorSessionNotFoundException;
import com.edutech.mentorsvc.application.exception.SessionAlreadyBookedException;
import com.edutech.mentorsvc.domain.event.MentorSessionBookedEvent;
import com.edutech.mentorsvc.domain.event.SessionCompletedEvent;
import com.edutech.mentorsvc.domain.model.MentorProfile;
import com.edutech.mentorsvc.domain.model.MentorSession;
import com.edutech.mentorsvc.domain.model.SessionMode;
import com.edutech.mentorsvc.domain.port.in.BookMentorSessionUseCase;
import com.edutech.mentorsvc.domain.port.in.CompleteSessionUseCase;
import com.edutech.mentorsvc.domain.port.in.GetMentorSessionUseCase;
import com.edutech.mentorsvc.domain.port.in.UpdateSessionStatusUseCase;
import com.edutech.mentorsvc.domain.model.SessionStatus;
import com.edutech.mentorsvc.domain.port.out.MentorEventPublisher;
import com.edutech.mentorsvc.domain.port.out.MentorProfileRepository;
import com.edutech.mentorsvc.domain.port.out.MentorSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MentorSessionService implements BookMentorSessionUseCase, GetMentorSessionUseCase,
        CompleteSessionUseCase, UpdateSessionStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(MentorSessionService.class);

    private final MentorSessionRepository mentorSessionRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final MentorEventPublisher mentorEventPublisher;

    public MentorSessionService(MentorSessionRepository mentorSessionRepository,
                                MentorProfileRepository mentorProfileRepository,
                                MentorEventPublisher mentorEventPublisher) {
        this.mentorSessionRepository = mentorSessionRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorEventPublisher = mentorEventPublisher;
    }

    @Override
    public MentorSessionResponse bookSession(BookSessionRequest request) {
        MentorProfile mentor = mentorProfileRepository.findById(request.mentorId())
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new MentorNotFoundException(request.mentorId()));

        if (!mentor.isAvailable()) {
            throw new SessionAlreadyBookedException(
                    "Mentor with id '" + request.mentorId() + "' is currently not available for booking.");
        }

        SessionMode mode = SessionMode.valueOf(request.sessionMode());
        MentorSession session = MentorSession.create(
                mentor,
                request.studentId(),
                request.scheduledAt(),
                request.durationMinutes(),
                mode,
                request.meetingLink(),
                request.notes()
        );

        MentorSession saved = mentorSessionRepository.save(session);

        try {
            MentorSessionBookedEvent event = MentorSessionBookedEvent.of(
                    saved.getId(),
                    mentor.getId(),
                    request.studentId(),
                    saved.getScheduledAt(),
                    saved.getDurationMinutes(),
                    saved.getSessionMode().name()
            );
            mentorEventPublisher.publishSessionBooked(event);
        } catch (Exception ex) {
            log.error("Failed to publish MentorSessionBookedEvent for session {}: {}",
                    saved.getId(), ex.getMessage(), ex);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorSessionResponse getSessionById(UUID sessionId) {
        MentorSession session = mentorSessionRepository.findById(sessionId)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new MentorSessionNotFoundException(sessionId));
        return toResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorSessionResponse> getSessionsByMentor(UUID mentorId) {
        return mentorSessionRepository.findByMentorId(mentorId)
                .stream()
                .filter(s -> s.getDeletedAt() == null)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorSessionResponse> getSessionsByStudent(UUID studentId) {
        return mentorSessionRepository.findByStudentId(studentId)
                .stream()
                .filter(s -> s.getDeletedAt() == null)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MentorSessionResponse completeSession(UUID sessionId) {
        MentorSession session = mentorSessionRepository.findById(sessionId)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new MentorSessionNotFoundException(sessionId));

        session.markCompleted();
        MentorSession saved = mentorSessionRepository.save(session);

        MentorProfile mentor = saved.getMentor();
        mentor.incrementTotalSessions();
        mentorProfileRepository.save(mentor);

        try {
            SessionCompletedEvent event = SessionCompletedEvent.of(
                    saved.getId(),
                    mentor.getId(),
                    saved.getStudentId(),
                    saved.getCompletedAt()
            );
            mentorEventPublisher.publishSessionCompleted(event);
        } catch (Exception ex) {
            log.error("Failed to publish SessionCompletedEvent for session {}: {}",
                    saved.getId(), ex.getMessage(), ex);
        }

        return toResponse(saved);
    }

    @Override
    public MentorSessionResponse updateStatus(UUID sessionId, SessionStatus newStatus) {
        MentorSession session = mentorSessionRepository.findById(sessionId)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new MentorSessionNotFoundException(sessionId));
        switch (newStatus) {
            case IN_PROGRESS -> session.markInProgress();
            case COMPLETED -> session.markCompleted();
            case CANCELLED -> session.markCancelled();
            case NO_SHOW -> session.markNoShow();
            default -> throw new IllegalArgumentException("Invalid status transition: " + newStatus);
        }
        MentorSession saved = mentorSessionRepository.save(session);
        return toResponse(saved);
    }

    private MentorSessionResponse toResponse(MentorSession session) {
        return new MentorSessionResponse(
                session.getId(),
                session.getMentor().getId(),
                session.getMentor().getFullName(),
                session.getStudentId(),
                session.getScheduledAt(),
                session.getDurationMinutes(),
                session.getSessionMode().name(),
                session.getStatus().name(),
                session.getMeetingLink(),
                session.getNotes(),
                session.getCreatedAt(),
                session.getCompletedAt()
        );
    }
}
