package com.edutech.chat.domain.port.in;

import java.util.UUID;

public interface DeleteSessionUseCase {
    void archiveSession(UUID sessionId, UUID userId);
}
