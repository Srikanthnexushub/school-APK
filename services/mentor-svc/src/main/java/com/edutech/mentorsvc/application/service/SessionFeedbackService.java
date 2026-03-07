package com.edutech.mentorsvc.application.service;

import com.edutech.mentorsvc.application.dto.SessionFeedbackResponse;
import com.edutech.mentorsvc.application.dto.SubmitFeedbackRequest;
import com.edutech.mentorsvc.application.exception.MentorSessionNotFoundException;
import com.edutech.mentorsvc.application.exception.SessionAlreadyBookedException;
import com.edutech.mentorsvc.domain.event.FeedbackSubmittedEvent;
import com.edutech.mentorsvc.domain.model.MentorSession;
import com.edutech.mentorsvc.domain.model.SessionFeedback;
import com.edutech.mentorsvc.domain.model.SessionStatus;
import com.edutech.mentorsvc.domain.port.in.SubmitFeedbackUseCase;
import com.edutech.mentorsvc.domain.port.out.MentorEventPublisher;
import com.edutech.mentorsvc.domain.port.out.MentorProfileRepository;
import com.edutech.mentorsvc.domain.port.out.MentorSessionRepository;
import com.edutech.mentorsvc.domain.port.out.SessionFeedbackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class SessionFeedbackService implements SubmitFeedbackUseCase {

    private static final Logger log = LoggerFactory.getLogger(SessionFeedbackService.class);

    private final SessionFeedbackRepository sessionFeedbackRepository;
    private final MentorSessionRepository mentorSessionRepository;
    private final MentorProfileRepository mentorProfileRepository;
    private final MentorEventPublisher mentorEventPublisher;

    public SessionFeedbackService(SessionFeedbackRepository sessionFeedbackRepository,
                                  MentorSessionRepository mentorSessionRepository,
                                  MentorProfileRepository mentorProfileRepository,
                                  MentorEventPublisher mentorEventPublisher) {
        this.sessionFeedbackRepository = sessionFeedbackRepository;
        this.mentorSessionRepository = mentorSessionRepository;
        this.mentorProfileRepository = mentorProfileRepository;
        this.mentorEventPublisher = mentorEventPublisher;
    }

    @Override
    public SessionFeedbackResponse submitFeedback(UUID sessionId, SubmitFeedbackRequest request) {
        MentorSession session = mentorSessionRepository.findById(sessionId)
                .filter(s -> s.getDeletedAt() == null)
                .orElseThrow(() -> new MentorSessionNotFoundException(sessionId));

        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new SessionAlreadyBookedException(
                    "Feedback can only be submitted for COMPLETED sessions. Session " + sessionId
                            + " is in status: " + session.getStatus());
        }

        if (sessionFeedbackRepository.existsBySessionId(sessionId)) {
            throw new SessionAlreadyBookedException(sessionId);
        }

        SessionFeedback feedback = SessionFeedback.create(
                session,
                request.studentId(),
                session.getMentor().getId(),
                request.rating(),
                request.comment()
        );

        SessionFeedback saved = sessionFeedbackRepository.save(feedback);

        session.getMentor().updateAverageRating(BigDecimal.valueOf(request.rating()));
        mentorProfileRepository.save(session.getMentor());

        try {
            FeedbackSubmittedEvent event = FeedbackSubmittedEvent.of(
                    saved.getId(),
                    sessionId,
                    saved.getMentorId(),
                    saved.getStudentId(),
                    saved.getRating()
            );
            mentorEventPublisher.publishFeedbackSubmitted(event);
        } catch (Exception ex) {
            log.error("Failed to publish FeedbackSubmittedEvent for session {}: {}",
                    sessionId, ex.getMessage(), ex);
        }

        return toResponse(saved);
    }

    private SessionFeedbackResponse toResponse(SessionFeedback feedback) {
        return new SessionFeedbackResponse(
                feedback.getId(),
                feedback.getSession().getId(),
                feedback.getStudentId(),
                feedback.getMentorId(),
                feedback.getRating(),
                feedback.getComment(),
                feedback.getCreatedAt()
        );
    }
}
