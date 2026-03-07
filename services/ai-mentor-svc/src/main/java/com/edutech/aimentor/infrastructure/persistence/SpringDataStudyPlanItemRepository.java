package com.edutech.aimentor.infrastructure.persistence;

import com.edutech.aimentor.domain.model.StudyPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataStudyPlanItemRepository extends JpaRepository<StudyPlanItem, UUID> {

    Optional<StudyPlanItem> findByIdAndDeletedAtIsNull(UUID id);
}
