package com.edutech.mentorsvc.domain.port.out;

import com.edutech.mentorsvc.domain.model.MentorSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MentorSessionRepository {
    MentorSession save(MentorSession session);
    Optional<MentorSession> findById(UUID id);
    List<MentorSession> findByMentorId(UUID mentorId);
    List<MentorSession> findByStudentId(UUID studentId);
}
