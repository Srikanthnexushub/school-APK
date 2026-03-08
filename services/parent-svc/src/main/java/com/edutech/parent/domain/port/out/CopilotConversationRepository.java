package com.edutech.parent.domain.port.out;

import com.edutech.parent.domain.model.CopilotConversation;

import java.util.List;
import java.util.Optional;

public interface CopilotConversationRepository {
    CopilotConversation save(CopilotConversation conversation);
    Optional<CopilotConversation> findById(Long id);
    List<CopilotConversation> findByParentId(String parentId);
}
