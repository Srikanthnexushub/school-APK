package com.edutech.chat.domain.port.in;

import com.edutech.chat.application.service.ChatSessionStartResult;
import java.util.UUID;

public interface StartSessionUseCase {
    ChatSessionStartResult startSession(UUID userId, String userRole, String pageContext, String jwtToken);
}
