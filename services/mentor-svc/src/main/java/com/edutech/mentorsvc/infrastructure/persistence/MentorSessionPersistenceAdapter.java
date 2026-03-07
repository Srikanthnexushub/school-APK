package com.edutech.mentorsvc.infrastructure.persistence;

import com.edutech.mentorsvc.domain.model.MentorSession;
import com.edutech.mentorsvc.domain.port.out.MentorSessionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MentorSessionPersistenceAdapter implements MentorSessionRepository {

    private final SpringDataMentorSessionRepository springDataRepository;

    public MentorSessionPersistenceAdapter(SpringDataMentorSessionRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public MentorSession save(MentorSession session) {
        return springDataRepository.save(session);
    }

    @Override
    public Optional<MentorSession> findById(UUID id) {
        return springDataRepository.findById(id);
    }

    @Override
    public List<MentorSession> findByMentorId(UUID mentorId) {
        return springDataRepository.findByMentorIdAndNotDeleted(mentorId);
    }

    @Override
    public List<MentorSession> findByStudentId(UUID studentId) {
        return springDataRepository.findByStudentIdAndNotDeleted(studentId);
    }
}
