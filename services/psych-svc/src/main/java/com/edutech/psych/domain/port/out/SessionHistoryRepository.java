package com.edutech.psych.domain.port.out;

import com.edutech.psych.domain.model.SessionHistory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionHistoryRepository {

    Optional<SessionHistory> findById(UUID id);

    List<SessionHistory> findByProfileId(UUID profileId);

    SessionHistory save(SessionHistory session);
}
