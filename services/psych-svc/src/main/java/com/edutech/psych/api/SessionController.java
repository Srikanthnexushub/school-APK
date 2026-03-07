package com.edutech.psych.api;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.application.dto.CompleteSessionRequest;
import com.edutech.psych.application.dto.SessionResponse;
import com.edutech.psych.application.dto.StartSessionRequest;
import com.edutech.psych.application.service.SessionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/psych/profiles/{profileId}/sessions")
@Tag(name = "Sessions", description = "Psychometric session management endpoints")
@SecurityRequirement(name = "BearerAuth")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> startSession(
            @PathVariable UUID profileId,
            @Valid @RequestBody StartSessionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        SessionResponse response = sessionService.startSession(profileId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<SessionResponse> completeSession(
            @PathVariable UUID profileId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CompleteSessionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        SessionResponse response = sessionService.completeSession(sessionId, request, principal);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> listSessions(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<SessionResponse> responses = sessionService.listSessions(profileId, principal);
        return ResponseEntity.ok(responses);
    }
}
