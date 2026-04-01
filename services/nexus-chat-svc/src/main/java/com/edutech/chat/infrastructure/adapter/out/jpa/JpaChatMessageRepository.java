package com.edutech.chat.infrastructure.adapter.out.jpa;

import com.edutech.chat.domain.model.ChatMessage;
import com.edutech.chat.domain.port.out.ChatMessageRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaChatMessageRepository
    extends JpaRepository<ChatMessage, UUID>, ChatMessageRepository {

    @Override
    @Query("SELECT m FROM ChatMessage m WHERE m.session.id = :sessionId ORDER BY m.createdAt ASC")
    List<ChatMessage> findAllBySessionId(UUID sessionId);

    @Override
    @Query(value = "SELECT * FROM chat_schema.chat_messages WHERE session_id = :sessionId " +
                   "ORDER BY created_at DESC LIMIT :n", nativeQuery = true)
    List<ChatMessage> findLastNBySessionId(UUID sessionId, int n);
}
