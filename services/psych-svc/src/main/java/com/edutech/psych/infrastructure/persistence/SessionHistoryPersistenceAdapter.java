package com.edutech.psych.infrastructure.persistence;

import com.edutech.psych.domain.model.SessionHistory;
import com.edutech.psych.domain.port.out.SessionHistoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SessionHistoryPersistenceAdapter implements SessionHistoryRepository {

    private final SpringDataSessionHistoryRepository springData;

    public SessionHistoryPersistenceAdapter(SpringDataSessionHistoryRepository springData) {
        this.springData = springData;
    }

    @Override
    public SessionHistory save(SessionHistory session) {
        return springData.save(session);
    }

    @Override
    public Optional<SessionHistory> findById(UUID id) {
        return springData.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<SessionHistory> findByProfileId(UUID profileId) {
        return springData.findByProfileIdAndDeletedAtIsNull(profileId);
    }

}
