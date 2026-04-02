package com.edutech.curricular.infrastructure.adapter.in.web;

import com.edutech.curricular.application.dto.*;
import com.edutech.curricular.domain.model.*;
import com.edutech.curricular.domain.port.in.QueryFrameworkUseCase;
import com.edutech.curricular.infrastructure.config.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/curricular")
@RequiredArgsConstructor
public class FrameworkController {

    private final QueryFrameworkUseCase frameworkUseCase;

    @GetMapping("/frameworks")
    public ResponseEntity<List<BoardResponse>> listActiveBoards(
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<BoardResponse> boards = frameworkUseCase.listActiveBoards().stream()
            .map(b -> new BoardResponse(b.getId(), b.getBoardCode(), b.getBoardName(), b.getCountryCode()))
            .toList();
        return ResponseEntity.ok(boards);
    }

    @GetMapping("/frameworks/{boardId}/subjects")
    public ResponseEntity<List<SubjectResponse>> listSubjects(
            @PathVariable UUID boardId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<SubjectResponse> subjects = frameworkUseCase.listSubjectsByBoard(boardId).stream()
            .map(s -> new SubjectResponse(s.getId(), s.getBoardId(), s.getSubjectCode(), s.getSubjectName()))
            .toList();
        return ResponseEntity.ok(subjects);
    }

    @GetMapping("/frameworks/{boardId}/grades")
    public ResponseEntity<List<GradeLevelResponse>> listGrades(
            @PathVariable UUID boardId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<GradeLevelResponse> grades = frameworkUseCase.listGradesByBoard(boardId).stream()
            .map(g -> new GradeLevelResponse(g.getId(), g.getBoardId(), g.getGradeCode(), g.getDisplayName(), g.getSortOrder()))
            .toList();
        return ResponseEntity.ok(grades);
    }

    @GetMapping("/frameworks/chapters")
    public ResponseEntity<List<ChapterResponse>> listChapters(
            @RequestParam UUID subjectId,
            @RequestParam UUID gradeId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<ChapterResponse> chapters = frameworkUseCase.listChapters(subjectId, gradeId).stream()
            .map(c -> new ChapterResponse(c.getId(), c.getSubjectId(), c.getGradeId(), c.getChapterNumber(), c.getChapterTitle(), c.getTotalEstHours()))
            .toList();
        return ResponseEntity.ok(chapters);
    }

    @GetMapping("/frameworks/topics")
    public ResponseEntity<List<TopicResponse>> listTopics(
            @RequestParam UUID chapterId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<TopicResponse> topics = frameworkUseCase.listTopics(chapterId).stream()
            .map(t -> new TopicResponse(t.getId(), t.getChapterId(), t.getTopicCode(), t.getTopicTitle(), t.getBloomsLevel().name(), t.getEstHours(), t.getSortOrder()))
            .toList();
        return ResponseEntity.ok(topics);
    }
}
