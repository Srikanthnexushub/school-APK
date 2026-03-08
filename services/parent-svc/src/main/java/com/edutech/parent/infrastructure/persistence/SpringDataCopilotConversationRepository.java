package com.edutech.parent.infrastructure.persistence;

import com.edutech.parent.domain.model.CopilotConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataCopilotConversationRepository extends JpaRepository<CopilotConversation, Long> {
    List<CopilotConversation> findByParentId(String parentId);
}
