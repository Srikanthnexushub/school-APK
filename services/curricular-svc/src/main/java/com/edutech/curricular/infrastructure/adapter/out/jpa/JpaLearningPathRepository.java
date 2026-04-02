package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.LearningPath;
import com.edutech.curricular.domain.port.out.LearningPathRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaLearningPathRepository extends JpaRepository<LearningPath, UUID>, LearningPathRepository {

    @Override
    Optional<LearningPath> findByStudentIdAndSubjectIdAndGradeId(UUID studentId, UUID subjectId, UUID gradeId);

    @Override
    List<LearningPath> findByStudentId(UUID studentId);
}
