package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.LearningPath;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningPathRepository {
    LearningPath save(LearningPath learningPath);
    Optional<LearningPath> findById(UUID id);
    Optional<LearningPath> findByStudentIdAndSubjectIdAndGradeId(UUID studentId, UUID subjectId, UUID gradeId);
    List<LearningPath> findByStudentId(UUID studentId);
}
