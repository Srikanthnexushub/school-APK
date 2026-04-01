package com.edutech.chat.domain.port.in;

import com.edutech.chat.domain.model.ChatMessage;
import com.edutech.chat.domain.model.ChatSession;
import java.util.List;
import java.util.UUID;

public interface GetSessionHistoryUseCase {
    List<ChatSession> getActiveSessions(UUID userId);
    List<ChatMessage> getMessages(UUID sessionId, UUID userId);
}
