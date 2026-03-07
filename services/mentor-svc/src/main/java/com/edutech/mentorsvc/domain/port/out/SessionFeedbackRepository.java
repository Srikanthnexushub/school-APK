package com.edutech.mentorsvc.domain.port.out;

import com.edutech.mentorsvc.domain.model.SessionFeedback;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionFeedbackRepository {
    SessionFeedback save(SessionFeedback feedback);
    Optional<SessionFeedback> findById(UUID id);
    Optional<SessionFeedback> findBySessionId(UUID sessionId);
    boolean existsBySessionId(UUID sessionId);
    List<SessionFeedback> findByMentorId(UUID mentorId);
    List<SessionFeedback> findByStudentId(UUID studentId);
}
