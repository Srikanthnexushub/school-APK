package com.edutech.curricular.domain.port.out;

import com.edutech.curricular.domain.model.Chapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChapterRepository {
    Chapter save(Chapter chapter);
    Optional<Chapter> findById(UUID id);
    List<Chapter> findBySubjectIdAndGradeId(UUID subjectId, UUID gradeId);
    List<Chapter> findBySubjectId(UUID subjectId);
}
