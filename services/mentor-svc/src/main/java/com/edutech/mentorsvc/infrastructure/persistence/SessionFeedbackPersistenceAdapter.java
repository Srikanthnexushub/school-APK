package com.edutech.mentorsvc.infrastructure.persistence;

import com.edutech.mentorsvc.domain.model.SessionFeedback;
import com.edutech.mentorsvc.domain.port.out.SessionFeedbackRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SessionFeedbackPersistenceAdapter implements SessionFeedbackRepository {

    private final SpringDataSessionFeedbackRepository springDataRepository;

    public SessionFeedbackPersistenceAdapter(SpringDataSessionFeedbackRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public SessionFeedback save(SessionFeedback feedback) {
        return springDataRepository.save(feedback);
    }

    @Override
    public Optional<SessionFeedback> findById(UUID id) {
        return springDataRepository.findById(id);
    }

    @Override
    public Optional<SessionFeedback> findBySessionId(UUID sessionId) {
        return springDataRepository.findBySessionId(sessionId);
    }

    @Override
    public boolean existsBySessionId(UUID sessionId) {
        return springDataRepository.existsBySessionId(sessionId);
    }

    @Override
    public List<SessionFeedback> findByMentorId(UUID mentorId) {
        return springDataRepository.findByMentorId(mentorId);
    }

    @Override
    public List<SessionFeedback> findByStudentId(UUID studentId) {
        return springDataRepository.findByStudentId(studentId);
    }
}
