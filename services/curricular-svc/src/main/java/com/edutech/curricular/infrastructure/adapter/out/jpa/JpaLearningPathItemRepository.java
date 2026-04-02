package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.LearningPathItem;
import com.edutech.curricular.domain.port.out.LearningPathItemRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaLearningPathItemRepository extends JpaRepository<LearningPathItem, UUID>, LearningPathItemRepository {

    @Override
    List<LearningPathItem> findByPathId(UUID pathId);

    @Override
    @Query("SELECT lpi FROM LearningPathItem lpi " +
           "JOIN LearningPath lp ON lpi.pathId = lp.id " +
           "WHERE lpi.id = :itemId AND lp.studentId = :studentId")
    Optional<LearningPathItem> findByIdAndPathStudentId(@Param("itemId") UUID itemId, @Param("studentId") UUID studentId);

    @Override
    @Modifying
    @Query("DELETE FROM LearningPathItem lpi WHERE lpi.pathId = :pathId")
    void deleteByPathId(@Param("pathId") UUID pathId);
}
