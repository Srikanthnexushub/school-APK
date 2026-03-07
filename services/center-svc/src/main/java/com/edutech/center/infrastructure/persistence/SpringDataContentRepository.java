// src/main/java/com/edutech/center/infrastructure/persistence/SpringDataContentRepository.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.ContentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface SpringDataContentRepository extends JpaRepository<ContentItem, UUID> {

    @Query("SELECT c FROM ContentItem c WHERE c.centerId = :centerId AND c.deletedAt IS NULL")
    List<ContentItem> findByCenterIdActive(UUID centerId);

    @Query("SELECT c FROM ContentItem c WHERE c.batchId = :batchId AND c.deletedAt IS NULL")
    List<ContentItem> findByBatchIdActive(UUID batchId);
}
