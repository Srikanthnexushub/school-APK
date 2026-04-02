package com.edutech.chat.infrastructure.adapter.in;

import com.edutech.chat.application.service.ChatSessionService;
import com.edutech.chat.infrastructure.adapter.in.dto.*;
import com.edutech.chat.infrastructure.config.AuthPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatSessionService chatSessionService;

    @PostMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StartSessionResponse> startSession(
            @RequestBody @Valid StartSessionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestHeader("Authorization") String authHeader) {

        String jwt = authHeader.replace("Bearer ", "");
        var result = chatSessionService.startSession(
            principal.userId(), principal.role(), principal.centerId(),
            request.pageContext(), jwt);

        String firstName = result.context().fullName() != null && !result.context().fullName().isBlank()
            ? result.context().fullName().split(" ")[0] : "there";
        return ResponseEntity.ok(new StartSessionResponse(
            result.sessionId(),
            result.greeting(),
            firstName
        ));
    }

    @PostMapping(value = "/sessions/{sessionId}/stream",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseBodyEmitter streamMessage(
            @PathVariable UUID sessionId,
            @RequestBody @Valid StreamMessageRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestHeader("Authorization") String authHeader) {

        String jwt = authHeader.replace("Bearer ", "");
        return chatSessionService.streamMessage(
            sessionId, principal.userId(), principal.centerId(),
            request.message(), request.pageContext(), jwt);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SendMessageResponse> sendMessage(
            @PathVariable UUID sessionId,
            @RequestBody @Valid SendMessageRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestHeader("Authorization") String authHeader) {

        String jwt = authHeader.replace("Bearer ", "");
        var result = chatSessionService.sendMessage(sessionId, principal.userId(),
            request.message(), request.pageContext(), jwt);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatSessionSummaryDto>> getSessions(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(chatSessionService.getSessionSummaries(principal.userId()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(chatSessionService.getMessages(sessionId, principal.userId()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        chatSessionService.deleteSession(sessionId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/nudges/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProactiveNudgeDto>> getPendingNudges(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(chatSessionService.getPendingNudges(principal.userId()));
    }

    @PutMapping("/nudges/{nudgeId}/open")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> openNudge(
            @PathVariable UUID nudgeId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        chatSessionService.markNudgeOpened(nudgeId, principal.userId());
        return ResponseEntity.ok().build();
    }
}
