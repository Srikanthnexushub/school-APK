package com.edutech.chat.domain.port.out;

import com.edutech.chat.domain.model.ProactiveNudge;
import java.util.UUID;

public interface ChatEventPublisherPort {
    void publishMessageSent(UUID sessionId, UUID userId, int tokenCount, int latencyMs);
    void publishNudgeNotification(ProactiveNudge nudge);
    void publishSessionStarted(UUID sessionId, UUID userId);
}
