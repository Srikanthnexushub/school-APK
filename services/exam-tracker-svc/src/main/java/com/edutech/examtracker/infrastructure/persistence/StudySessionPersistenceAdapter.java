package com.edutech.examtracker.infrastructure.persistence;

import com.edutech.examtracker.domain.model.StudySession;
import com.edutech.examtracker.domain.port.out.StudySessionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class StudySessionPersistenceAdapter implements StudySessionRepository {

    private final SpringDataStudySessionRepository jpa;

    public StudySessionPersistenceAdapter(SpringDataStudySessionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public StudySession save(StudySession session) {
        return jpa.save(session);
    }

    @Override
    public Optional<StudySession> findById(UUID id) {
        return jpa.findByIdActive(id);
    }

    @Override
    public List<StudySession> findByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId) {
        return jpa.findByStudentIdAndEnrollmentIdActive(studentId, enrollmentId);
    }

    @Override
    public List<StudySession> findByStudentIdAndSessionDate(UUID studentId, LocalDate sessionDate) {
        return jpa.findByStudentIdAndSessionDateActive(studentId, sessionDate);
    }
}
