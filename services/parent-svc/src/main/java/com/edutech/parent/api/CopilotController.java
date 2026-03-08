package com.edutech.parent.api;

import com.edutech.parent.application.dto.AuthPrincipal;
import com.edutech.parent.application.dto.ContinueConversationRequest;
import com.edutech.parent.application.dto.ConversationResponse;
import com.edutech.parent.application.dto.StartConversationRequest;
import com.edutech.parent.application.service.CopilotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for Parent Copilot — the AI chatbot that helps parents understand
 * their child's academic progress, fees, attendance, and next steps.
 */
@RestController
@RequestMapping("/api/v1/copilot/conversations")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Parent Copilot", description = "AI-powered chat assistant for parents")
public class CopilotController {

    private final CopilotService copilotService;

    public CopilotController(CopilotService copilotService) {
        this.copilotService = copilotService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a new Copilot conversation")
    public ConversationResponse startConversation(
            @Valid @RequestBody StartConversationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return copilotService.startConversation(
                principal.userId().toString(),
                request.studentId(),
                request.message());
    }

    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "Send a follow-up message in an existing conversation")
    public ConversationResponse continueConversation(
            @PathVariable Long conversationId,
            @Valid @RequestBody ContinueConversationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return copilotService.continueConversation(
                conversationId,
                principal.userId().toString(),
                request.message());
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "Get a Copilot conversation with all messages")
    public ConversationResponse getConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return copilotService.getConversation(conversationId, principal.userId().toString());
    }

    @DeleteMapping("/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Close a Copilot conversation")
    public void closeConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        copilotService.closeConversation(conversationId, principal.userId().toString());
    }
}
