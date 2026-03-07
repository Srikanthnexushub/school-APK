// src/main/java/com/edutech/center/domain/port/out/ContentRepository.java
package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.ContentItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentRepository {
    ContentItem save(ContentItem item);
    Optional<ContentItem> findById(UUID id);
    List<ContentItem> findByCenterId(UUID centerId);
    List<ContentItem> findByBatchId(UUID batchId);
}
