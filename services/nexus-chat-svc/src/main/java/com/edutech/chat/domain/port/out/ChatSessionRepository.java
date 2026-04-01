package com.edutech.chat.domain.port.out;

import com.edutech.chat.domain.model.ChatSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository {
    ChatSession save(ChatSession session);
    Optional<ChatSession> findByIdAndUserId(UUID sessionId, UUID userId);
    List<ChatSession> findActiveByUserId(UUID userId);
    Optional<ChatSession> findById(UUID sessionId);
}
