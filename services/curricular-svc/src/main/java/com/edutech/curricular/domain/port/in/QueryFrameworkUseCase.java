package com.edutech.curricular.domain.port.in;

import com.edutech.curricular.domain.model.Chapter;
import com.edutech.curricular.domain.model.CurriculumBoard;
import com.edutech.curricular.domain.model.GradeLevel;
import com.edutech.curricular.domain.model.Subject;
import com.edutech.curricular.domain.model.Topic;

import java.util.List;
import java.util.UUID;

public interface QueryFrameworkUseCase {
    List<CurriculumBoard> listActiveBoards();
    List<Subject> listSubjectsByBoard(UUID boardId);
    List<GradeLevel> listGradesByBoard(UUID boardId);
    List<Chapter> listChapters(UUID subjectId, UUID gradeId);
    List<Topic> listTopics(UUID chapterId);
}
