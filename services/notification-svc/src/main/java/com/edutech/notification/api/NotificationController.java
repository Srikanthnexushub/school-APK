package com.edutech.notification.api;

import com.edutech.notification.application.dto.NotificationHistoryResponse;
import com.edutech.notification.domain.port.in.GetNotificationHistoryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final GetNotificationHistoryUseCase getNotificationHistoryUseCase;

    public NotificationController(GetNotificationHistoryUseCase getNotificationHistoryUseCase) {
        this.getNotificationHistoryUseCase = getNotificationHistoryUseCase;
    }

    @GetMapping
    @Operation(summary = "Get notification history for the authenticated user")
    public ResponseEntity<Page<NotificationHistoryResponse>> getMyNotifications(
            @AuthenticationPrincipal String userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<NotificationHistoryResponse> page =
                getNotificationHistoryUseCase.getHistory(UUID.fromString(userId), pageable);
        return ResponseEntity.ok(page);
    }
}
