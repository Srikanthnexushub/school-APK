package com.edutech.psych.infrastructure.persistence;

import com.edutech.psych.domain.model.SessionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataSessionHistoryRepository extends JpaRepository<SessionHistory, UUID> {

    Optional<SessionHistory> findByIdAndDeletedAtIsNull(UUID id);

    List<SessionHistory> findByProfileIdAndDeletedAtIsNull(UUID profileId);
}
