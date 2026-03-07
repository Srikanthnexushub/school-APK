package com.edutech.examtracker.application.service;

import com.edutech.examtracker.application.dto.RecordStudySessionRequest;
import com.edutech.examtracker.application.dto.StudySessionResponse;
import com.edutech.examtracker.domain.event.StudySessionRecordedEvent;
import com.edutech.examtracker.domain.model.StudySession;
import com.edutech.examtracker.domain.port.in.GetStudySessionsUseCase;
import com.edutech.examtracker.domain.port.in.RecordStudySessionUseCase;
import com.edutech.examtracker.domain.port.out.ExamTrackerEventPublisher;
import com.edutech.examtracker.domain.port.out.StudySessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class StudySessionService implements RecordStudySessionUseCase, GetStudySessionsUseCase {

    private static final Logger log = LoggerFactory.getLogger(StudySessionService.class);

    private final StudySessionRepository studySessionRepository;
    private final ExamTrackerEventPublisher eventPublisher;

    public StudySessionService(StudySessionRepository studySessionRepository,
                               ExamTrackerEventPublisher eventPublisher) {
        this.studySessionRepository = studySessionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public StudySessionResponse recordSession(UUID studentId, RecordStudySessionRequest request) {
        StudySession session = StudySession.create(
                studentId,
                request.enrollmentId(),
                request.subject(),
                request.topicName(),
                request.sessionType(),
                request.sessionDate(),
                request.durationMinutes()
        );

        if (request.questionsAttempted() != null) {
            session.setQuestionsAttempted(request.questionsAttempted());
        }
        if (request.accuracyPercent() != null) {
            session.setAccuracyPercent(request.accuracyPercent());
        }
        if (request.notes() != null) {
            session.setNotes(request.notes());
        }

        StudySession saved = studySessionRepository.save(session);

        eventPublisher.publish(new StudySessionRecordedEvent(
                UUID.randomUUID().toString(),
                studentId,
                request.enrollmentId(),
                request.subject(),
                request.topicName(),
                saved.getDurationMinutes(),
                saved.getAccuracyPercent(),
                Instant.now()
        ));

        log.info("Study session recorded: studentId={} subject={} sessionId={}",
                studentId, request.subject(), saved.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudySessionResponse> getStudySessions(UUID studentId, UUID enrollmentId) {
        return studySessionRepository.findByStudentIdAndEnrollmentId(studentId, enrollmentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private StudySessionResponse toResponse(StudySession s) {
        return new StudySessionResponse(
                s.getId(),
                s.getStudentId(),
                s.getEnrollmentId(),
                s.getSubject(),
                s.getTopicName(),
                s.getSessionType(),
                s.getSessionDate(),
                s.getDurationMinutes(),
                s.getQuestionsAttempted(),
                s.getAccuracyPercent(),
                s.getNotes(),
                s.getCreatedAt()
        );
    }
}
