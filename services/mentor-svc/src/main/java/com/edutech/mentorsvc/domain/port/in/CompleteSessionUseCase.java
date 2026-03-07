package com.edutech.mentorsvc.domain.port.in;

import com.edutech.mentorsvc.application.dto.MentorSessionResponse;

import java.util.UUID;

public interface CompleteSessionUseCase {
    MentorSessionResponse completeSession(UUID sessionId);
}
