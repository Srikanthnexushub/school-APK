package com.edutech.notification.api;

import com.edutech.notification.application.dto.NotificationHistoryResponse;
import com.edutech.notification.domain.port.in.GetNotificationHistoryUseCase;
import com.edutech.notification.domain.port.in.MarkNotificationReadUseCase;
import com.edutech.notification.infrastructure.sse.SseEmitterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final GetNotificationHistoryUseCase getNotificationHistoryUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final SseEmitterRegistry sseEmitterRegistry;

    public NotificationController(GetNotificationHistoryUseCase getNotificationHistoryUseCase,
                                   MarkNotificationReadUseCase markNotificationReadUseCase,
                                   SseEmitterRegistry sseEmitterRegistry) {
        this.getNotificationHistoryUseCase = getNotificationHistoryUseCase;
        this.markNotificationReadUseCase = markNotificationReadUseCase;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    @GetMapping
    @Operation(summary = "Get full notification history for the authenticated user")
    public ResponseEntity<Page<NotificationHistoryResponse>> getMyNotifications(
            @AuthenticationPrincipal String userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(
                getNotificationHistoryUseCase.getHistory(UUID.fromString(userId), pageable));
    }

    @GetMapping("/inapp")
    @Operation(summary = "Get IN_APP notifications for the notification panel (newest first)")
    public ResponseEntity<Page<NotificationHistoryResponse>> getInAppNotifications(
            @AuthenticationPrincipal String userId,
            @PageableDefault(size = 30, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(
                getNotificationHistoryUseCase.getInAppNotifications(UUID.fromString(userId), pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Returns the count of unread IN_APP notifications")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal String userId) {
        long count = getNotificationHistoryUseCase.countUnreadInApp(UUID.fromString(userId));
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a single IN_APP notification as read")
    public ResponseEntity<Void> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId) {
        markNotificationReadUseCase.markRead(id, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/mark-all-read")
    @Operation(summary = "Mark all IN_APP notifications as read for the authenticated user")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal String userId) {
        markNotificationReadUseCase.markAllRead(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    /**
     * Server-Sent Events stream for real-time in-app notification delivery.
     * The client subscribes once; new notifications are pushed without polling.
     * Authentication: Bearer token is sent by the frontend via fetch() — the API gateway
     * validates the JWT and injects X-User-Id, which GatewayHeaderAuthFilter reads.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE stream — subscribe to receive real-time IN_APP notifications")
    public SseEmitter streamNotifications(@AuthenticationPrincipal String userId) {
        return sseEmitterRegistry.subscribe(UUID.fromString(userId));
    }
}
