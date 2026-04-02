package com.edutech.curricular.infrastructure.adapter.out.jpa;

import com.edutech.curricular.domain.model.Chapter;
import com.edutech.curricular.domain.port.out.ChapterRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaChapterRepository extends JpaRepository<Chapter, UUID>, ChapterRepository {

    @Override
    List<Chapter> findBySubjectIdAndGradeId(UUID subjectId, UUID gradeId);

    @Override
    List<Chapter> findBySubjectId(UUID subjectId);
}
