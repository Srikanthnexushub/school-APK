package com.edutech.mentorsvc.domain.port.in;

import com.edutech.mentorsvc.application.dto.SessionFeedbackResponse;
import com.edutech.mentorsvc.application.dto.SubmitFeedbackRequest;

import java.util.UUID;

public interface SubmitFeedbackUseCase {
    SessionFeedbackResponse submitFeedback(UUID sessionId, SubmitFeedbackRequest request);
}
