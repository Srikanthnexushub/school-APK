package com.edutech.mentorsvc.domain.port.in;

import com.edutech.mentorsvc.application.dto.MentorSessionResponse;
import com.edutech.mentorsvc.domain.model.SessionStatus;

import java.util.UUID;

public interface UpdateSessionStatusUseCase {
    MentorSessionResponse updateStatus(UUID sessionId, SessionStatus newStatus);
}
