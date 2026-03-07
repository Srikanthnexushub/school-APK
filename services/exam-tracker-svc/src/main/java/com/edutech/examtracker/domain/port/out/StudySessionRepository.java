package com.edutech.examtracker.domain.port.out;

import com.edutech.examtracker.domain.model.StudySession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudySessionRepository {

    StudySession save(StudySession session);

    Optional<StudySession> findById(UUID id);

    List<StudySession> findByStudentIdAndEnrollmentId(UUID studentId, UUID enrollmentId);

    List<StudySession> findByStudentIdAndSessionDate(UUID studentId, LocalDate sessionDate);
}
