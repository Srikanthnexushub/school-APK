// src/main/java/com/edutech/center/infrastructure/persistence/ContentPersistenceAdapter.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.ContentItem;
import com.edutech.center.domain.port.out.ContentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ContentPersistenceAdapter implements ContentRepository {

    private final SpringDataContentRepository jpa;

    public ContentPersistenceAdapter(SpringDataContentRepository jpa) { this.jpa = jpa; }

    @Override public ContentItem save(ContentItem item) { return jpa.save(item); }
    @Override public Optional<ContentItem> findById(UUID id) { return jpa.findById(id); }
    @Override public List<ContentItem> findByCenterId(UUID centerId) { return jpa.findByCenterIdActive(centerId); }
    @Override public List<ContentItem> findByBatchId(UUID batchId) { return jpa.findByBatchIdActive(batchId); }
}
