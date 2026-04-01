package com.edutech.chat.domain.port.out;

import com.edutech.chat.domain.model.ProactiveNudge;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProactiveNudgeRepository {
    ProactiveNudge save(ProactiveNudge nudge);
    List<ProactiveNudge> findUndeliveredByUserId(UUID userId, int limit);
    Optional<ProactiveNudge> findById(UUID nudgeId);
}
