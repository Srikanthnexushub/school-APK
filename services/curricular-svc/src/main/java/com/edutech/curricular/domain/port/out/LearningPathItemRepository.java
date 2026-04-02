package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.LearningPathItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningPathItemRepository {
    LearningPathItem save(LearningPathItem item);
    List<LearningPathItem> findByPathId(UUID pathId);
    Optional<LearningPathItem> findById(UUID id);
    Optional<LearningPathItem> findByIdAndPathStudentId(UUID itemId, UUID studentId);
    void deleteByPathId(UUID pathId);
}
