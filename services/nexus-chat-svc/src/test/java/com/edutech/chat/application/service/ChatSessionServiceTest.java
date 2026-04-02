package com.edutech.chat.application.service;

import com.edutech.chat.domain.model.*;
import com.edutech.chat.domain.port.out.*;
import com.edutech.chat.infrastructure.adapter.in.dto.ChatMessageDto;
import com.edutech.chat.infrastructure.adapter.in.dto.ChatSessionSummaryDto;
import com.edutech.chat.infrastructure.adapter.in.dto.ProactiveNudgeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatSessionService Unit Tests")
class ChatSessionServiceTest {

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID USER_ID    = UUID.randomUUID();
    private static final UUID CENTER_ID  = UUID.randomUUID();
    private static final String JWT      = "test-jwt";
    private static final String PAGE_CTX = "dashboard";

    @Mock ChatSessionRepository sessionRepo;
    @Mock ChatMessageRepository messageRepo;
    @Mock ProactiveNudgeRepository nudgeRepo;
    @Mock ContextAggregatorService contextAggregator;
    @Mock SystemPromptBuilder promptBuilder;
    @Mock AiGatewayStreamPort aiGateway;
    @Mock ChatEventPublisherPort eventPublisher;
    @Mock ExecutorService streamExecutor;

    @InjectMocks
    ChatSessionService service;

    private ChatSession mockSession() {
        ChatSession session = mock(ChatSession.class);
        when(session.getId()).thenReturn(SESSION_ID);
        when(session.getUserRole()).thenReturn("STUDENT");
        when(session.getMessageCount()).thenReturn(0);
        when(session.getLastActiveAt()).thenReturn(Instant.now());
        return session;
    }

    private StudentContext studentContext() {
        return StudentContext.empty(USER_ID, PAGE_CTX);
    }

    // ─── startSession ────────────────────────────

    @Test
    @DisplayName("startSession: creates session, persists system prompt + greeting, publishes event")
    void startSession_creates_session_and_publishes_event() {
        ChatSession session = mockSession();
        when(sessionRepo.save(any(ChatSession.class))).thenReturn(session);
        when(contextAggregator.aggregate(any(), anyString(), any(), anyString(), anyString()))
            .thenReturn(studentContext());
        when(promptBuilder.build(any(RoleContext.class))).thenReturn("system-prompt");
        when(messageRepo.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));

        ChatSessionStartResult result = service.startSession(
            USER_ID, "STUDENT", CENTER_ID, PAGE_CTX, JWT);

        assertThat(result).isNotNull();
        assertThat(result.sessionId()).isEqualTo(SESSION_ID);
        assertThat(result.greeting()).isNotBlank();
        // system message + greeting = 2 saves
        verify(messageRepo, atLeast(2)).save(any(ChatMessage.class));
        verify(eventPublisher).publishSessionStarted(SESSION_ID, USER_ID);
    }

    @Test
    @DisplayName("startSession TEACHER role: greeting contains teacher-specific text")
    void startSession_teacher_role_greeting() {
        ChatSession session = mockSession();
        when(session.getUserRole()).thenReturn("TEACHER");
        when(sessionRepo.save(any(ChatSession.class))).thenReturn(session);

        TeacherContext teacherCtx = new TeacherContext(
            USER_ID, "Priya Sharma", "Bio", List.of(), null, CENTER_ID, PAGE_CTX
        );
        when(contextAggregator.aggregate(any(), anyString(), any(), anyString(), anyString()))
            .thenReturn(teacherCtx);
        when(promptBuilder.build(any(RoleContext.class))).thenReturn("teacher-prompt");
        when(messageRepo.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));

        ChatSessionStartResult result = service.startSession(
            USER_ID, "TEACHER", CENTER_ID, PAGE_CTX, JWT);

        assertThat(result.greeting()).contains("Priya");
        assertThat(result.greeting()).containsIgnoringCase("teaching assistant");
    }

    @Test
    @DisplayName("startSession PARENT role: greeting contains parent-specific text")
    void startSession_parent_role_greeting() {
        ChatSession session = mockSession();
        when(session.getUserRole()).thenReturn("PARENT");
        when(sessionRepo.save(any(ChatSession.class))).thenReturn(session);

        ParentContext parentCtx = new ParentContext(
            USER_ID, "Meena Patel", List.of("Arjun"), PAGE_CTX
        );
        when(contextAggregator.aggregate(any(), anyString(), any(), anyString(), anyString()))
            .thenReturn(parentCtx);
        when(promptBuilder.build(any(RoleContext.class))).thenReturn("parent-prompt");
        when(messageRepo.save(any(ChatMessage.class))).thenAnswer(i -> i.getArgument(0));

        ChatSessionStartResult result = service.startSession(
            USER_ID, "PARENT", null, PAGE_CTX, JWT);

        assertThat(result.greeting()).contains("Meena");
        assertThat(result.greeting()).containsIgnoringCase("child");
    }

    // ─── getSessionSummaries ─────────────────────

    @Test
    @DisplayName("getSessionSummaries: maps active sessions to DTOs")
    void getSessionSummaries_maps_correctly() {
        ChatSession session = mockSession();
        when(session.getTitle()).thenReturn("My first chat");
        when(sessionRepo.findActiveByUserId(USER_ID)).thenReturn(List.of(session));

        List<ChatSessionSummaryDto> result = service.getSessionSummaries(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(SESSION_ID);
        assertThat(result.get(0).title()).isEqualTo("My first chat");
    }

    @Test
    @DisplayName("getSessionSummaries: returns empty list when no active sessions")
    void getSessionSummaries_empty() {
        when(sessionRepo.findActiveByUserId(USER_ID)).thenReturn(List.of());
        List<ChatSessionSummaryDto> result = service.getSessionSummaries(USER_ID);
        assertThat(result).isEmpty();
    }

    // ─── getMessages ─────────────────────────────

    @Test
    @DisplayName("getMessages: filters out SYSTEM role messages from results")
    void getMessages_filters_system_messages() {
        ChatSession session = mockSession();
        when(sessionRepo.findByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(Optional.of(session));

        ChatMessage systemMsg = mock(ChatMessage.class);
        when(systemMsg.getRole()).thenReturn(MessageRole.SYSTEM);
        when(systemMsg.getContent()).thenReturn("system-prompt");

        ChatMessage userMsg = mock(ChatMessage.class);
        when(userMsg.getId()).thenReturn(UUID.randomUUID());
        when(userMsg.getRole()).thenReturn(MessageRole.USER);
        when(userMsg.getContent()).thenReturn("Hello");
        when(userMsg.getMessageType()).thenReturn(MessageType.TEXT);
        when(userMsg.getCreatedAt()).thenReturn(Instant.now());

        when(messageRepo.findAllBySessionId(SESSION_ID))
            .thenReturn(List.of(systemMsg, userMsg));

        List<ChatMessageDto> result = service.getMessages(SESSION_ID, USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("getMessages: throws SessionNotFoundException when session does not belong to user")
    void getMessages_session_not_found() {
        when(sessionRepo.findByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMessages(SESSION_ID, USER_ID))
            .isInstanceOf(SessionNotFoundException.class);
    }

    // ─── deleteSession ────────────────────────────

    @Test
    @DisplayName("deleteSession: archives session (soft-delete, not hard-delete)")
    void deleteSession_archives_not_hard_deletes() {
        ChatSession session = mockSession();
        when(sessionRepo.findByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(Optional.of(session));
        when(sessionRepo.save(any())).thenReturn(session);

        service.deleteSession(SESSION_ID, USER_ID);

        verify(session).archive();
        verify(sessionRepo).save(session);
    }

    @Test
    @DisplayName("deleteSession: throws SessionNotFoundException for wrong user")
    void deleteSession_wrong_user_throws() {
        when(sessionRepo.findByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSession(SESSION_ID, USER_ID))
            .isInstanceOf(SessionNotFoundException.class);
    }

    // ─── getPendingNudges ─────────────────────────

    @Test
    @DisplayName("getPendingNudges: maps nudges to DTOs")
    void getPendingNudges_maps_nudges() {
        ProactiveNudge nudge = mock(ProactiveNudge.class);
        when(nudge.getId()).thenReturn(UUID.randomUUID());
        when(nudge.getMessage()).thenReturn("You have a weak area in Physics!");
        when(nudge.getActionUrl()).thenReturn("/performance");
        when(nudge.getTriggerType()).thenReturn(NudgeTriggerType.WEAK_AREA_CRITICAL);
        when(nudge.getCreatedAt()).thenReturn(Instant.now());

        when(nudgeRepo.findUndeliveredByUserId(USER_ID, 10)).thenReturn(List.of(nudge));

        List<ProactiveNudgeDto> result = service.getPendingNudges(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("You have a weak area in Physics!");
        assertThat(result.get(0).triggerType()).isEqualTo("WEAK_AREA_CRITICAL");
    }
}
