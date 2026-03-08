package com.edutech.parent.infrastructure.persistence;

import com.edutech.parent.domain.model.CopilotConversation;
import com.edutech.parent.domain.port.out.CopilotConversationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class CopilotConversationPersistenceAdapter implements CopilotConversationRepository {

    private final SpringDataCopilotConversationRepository repository;

    CopilotConversationPersistenceAdapter(SpringDataCopilotConversationRepository repository) {
        this.repository = repository;
    }

    @Override
    public CopilotConversation save(CopilotConversation conversation) {
        return repository.save(conversation);
    }

    @Override
    public Optional<CopilotConversation> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<CopilotConversation> findByParentId(String parentId) {
        return repository.findByParentId(parentId);
    }
}
