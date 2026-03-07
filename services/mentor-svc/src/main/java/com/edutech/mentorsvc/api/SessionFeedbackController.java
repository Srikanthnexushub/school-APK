package com.edutech.mentorsvc.api;

import com.edutech.mentorsvc.application.dto.SessionFeedbackResponse;
import com.edutech.mentorsvc.application.dto.SubmitFeedbackRequest;
import com.edutech.mentorsvc.domain.port.in.SubmitFeedbackUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mentor-sessions/{sessionId}/feedback")
@Tag(name = "Session Feedback", description = "Submit feedback for completed mentor sessions")
public class SessionFeedbackController {

    private final SubmitFeedbackUseCase submitFeedbackUseCase;

    public SessionFeedbackController(SubmitFeedbackUseCase submitFeedbackUseCase) {
        this.submitFeedbackUseCase = submitFeedbackUseCase;
    }

    @PostMapping
    @Operation(summary = "Submit feedback for a completed session")
    public ResponseEntity<SessionFeedbackResponse> submitFeedback(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitFeedbackRequest request) {
        SessionFeedbackResponse response = submitFeedbackUseCase.submitFeedback(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
