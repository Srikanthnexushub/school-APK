package com.edutech.chat.infrastructure.adapter.out.jpa;

import com.edutech.chat.domain.model.ProactiveNudge;
import com.edutech.chat.domain.port.out.ProactiveNudgeRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaProactiveNudgeRepository
    extends JpaRepository<ProactiveNudge, UUID>, ProactiveNudgeRepository {

    @Override
    @Query(value = "SELECT * FROM chat_schema.proactive_nudges WHERE user_id = :userId AND opened = false " +
           "ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<ProactiveNudge> findUndeliveredByUserId(UUID userId, int limit);

    @Override
    Optional<ProactiveNudge> findById(UUID nudgeId);
}
