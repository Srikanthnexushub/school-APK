package com.edutech.chat.domain.port.in;

import com.edutech.chat.domain.model.ProactiveNudge;
import java.util.List;
import java.util.UUID;

public interface GetPendingNudgesUseCase {
    List<ProactiveNudge> getPendingNudges(UUID userId, int limit);
    void markNudgeOpened(UUID nudgeId, UUID userId);
}
