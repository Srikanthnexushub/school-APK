package com.edutech.mentorsvc.domain.port.in;

import com.edutech.mentorsvc.application.dto.MentorSessionResponse;

import java.util.List;
import java.util.UUID;

public interface GetMentorSessionUseCase {
    MentorSessionResponse getSessionById(UUID sessionId);
    List<MentorSessionResponse> getSessionsByMentor(UUID mentorId);
    List<MentorSessionResponse> getSessionsByStudent(UUID studentId);
}
