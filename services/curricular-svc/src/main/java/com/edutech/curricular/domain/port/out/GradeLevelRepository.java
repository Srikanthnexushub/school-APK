package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.GradeLevel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeLevelRepository {
    GradeLevel save(GradeLevel gradeLevel);
    Optional<GradeLevel> findById(UUID id);
    List<GradeLevel> findByBoardId(UUID boardId);
}
