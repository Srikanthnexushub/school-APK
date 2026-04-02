package com.edutech.curricular.application.service;

import com.edutech.curricular.domain.model.Chapter;
import com.edutech.curricular.domain.model.CurriculumBoard;
import com.edutech.curricular.domain.model.GradeLevel;
import com.edutech.curricular.domain.model.Subject;
import com.edutech.curricular.domain.model.Topic;
import com.edutech.curricular.domain.port.in.QueryFrameworkUseCase;
import com.edutech.curricular.domain.port.out.ChapterRepository;
import com.edutech.curricular.domain.port.out.CurriculumBoardRepository;
import com.edutech.curricular.domain.port.out.GradeLevelRepository;
import com.edutech.curricular.domain.port.out.SubjectRepository;
import com.edutech.curricular.domain.port.out.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FrameworkQueryService implements QueryFrameworkUseCase {

    private final CurriculumBoardRepository boardRepository;
    private final SubjectRepository subjectRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final ChapterRepository chapterRepository;
    private final TopicRepository topicRepository;

    @Override
    public List<CurriculumBoard> listActiveBoards() {
        return boardRepository.findByActive(true);
    }

    @Override
    public List<Subject> listSubjectsByBoard(UUID boardId) {
        return subjectRepository.findByBoardId(boardId).stream()
            .filter(Subject::isActive)
            .toList();
    }

    @Override
    public List<GradeLevel> listGradesByBoard(UUID boardId) {
        return gradeLevelRepository.findByBoardId(boardId);
    }

    @Override
    public List<Chapter> listChapters(UUID subjectId, UUID gradeId) {
        return chapterRepository.findBySubjectIdAndGradeId(subjectId, gradeId).stream()
            .filter(Chapter::isActive)
            .toList();
    }

    @Override
    public List<Topic> listTopics(UUID chapterId) {
        return topicRepository.findByChapterId(chapterId).stream()
            .filter(Topic::isActive)
            .toList();
    }
}
