package com.edutech.chat.infrastructure.adapter.out.jpa;

import com.edutech.chat.domain.model.ChatSession;
import com.edutech.chat.domain.port.out.ChatSessionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaChatSessionRepository
    extends JpaRepository<ChatSession, UUID>, ChatSessionRepository {

    @Override
    @Query("SELECT s FROM ChatSession s WHERE s.id = :id AND s.userId = :userId AND s.deletedAt IS NULL")
    Optional<ChatSession> findByIdAndUserId(UUID id, UUID userId);

    @Override
    @Query("SELECT s FROM ChatSession s WHERE s.userId = :userId AND s.status = 'ACTIVE' " +
           "AND s.deletedAt IS NULL ORDER BY s.lastActiveAt DESC")
    List<ChatSession> findActiveByUserId(UUID userId);
}
