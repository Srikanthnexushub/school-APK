package com.edutech.chat.domain.port.out;

import com.edutech.chat.domain.model.ChatMessage;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage message);
    List<ChatMessage> findLastNBySessionId(UUID sessionId, int n);
    List<ChatMessage> findAllBySessionId(UUID sessionId);
}
