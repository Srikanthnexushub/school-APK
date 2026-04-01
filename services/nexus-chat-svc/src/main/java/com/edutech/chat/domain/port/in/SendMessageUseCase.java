package com.edutech.chat.domain.port.in;

import com.edutech.chat.domain.model.ChatMessage;
import java.util.Map;
import java.util.UUID;

public interface SendMessageUseCase {
    ChatMessage sendMessage(UUID sessionId, UUID userId, String content,
                            String messageType, Map<String, Object> actionPayload);
}
