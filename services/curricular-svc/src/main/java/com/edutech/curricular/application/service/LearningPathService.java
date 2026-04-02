package com.edutech.curricular.application.service;

import com.edutech.curricular.domain.model.Chapter;
import com.edutech.curricular.domain.model.LearningPath;
import com.edutech.curricular.domain.model.LearningPathItem;
import com.edutech.curricular.domain.model.Topic;
import com.edutech.curricular.domain.model.TopicPrerequisite;
import com.edutech.curricular.domain.model.enums.PathItemStatus;
import com.edutech.curricular.domain.port.in.ComputeLearningPathUseCase;
import com.edutech.curricular.domain.port.in.UpdateLearningPathUseCase;
import com.edutech.curricular.domain.port.out.ChapterRepository;
import com.edutech.curricular.domain.port.out.CurricularEventPublisher;
import com.edutech.curricular.domain.port.out.LearningPathItemRepository;
import com.edutech.curricular.domain.port.out.LearningPathRepository;
import com.edutech.curricular.domain.port.out.MasteryDataPort;
import com.edutech.curricular.domain.port.out.TopicPrerequisiteRepository;
import com.edutech.curricular.domain.port.out.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningPathService implements ComputeLearningPathUseCase, UpdateLearningPathUseCase {

    private final LearningPathRepository learningPathRepository;
    private final LearningPathItemRepository learningPathItemRepository;
    private final ChapterRepository chapterRepository;
    private final TopicRepository topicRepository;
    private final TopicPrerequisiteRepository topicPrerequisiteRepository;
    private final MasteryDataPort masteryDataPort;
    private final TopologicalSortService topologicalSortService;
    private final UrgencyScoreCalculator urgencyScoreCalculator;
    private final CurricularEventPublisher eventPublisher;

    @Override
    @Transactional
    public LearningPath computeOrRefresh(UUID studentId, UUID subjectId, UUID gradeId, String jwt) {
        LearningPath path = learningPathRepository
            .findByStudentIdAndSubjectIdAndGradeId(studentId, subjectId, gradeId)
            .orElseGet(() -> LearningPath.create(studentId, subjectId, gradeId));

        // If existing path, remove old items
        if (path.getId() != null) {
            learningPathItemRepository.deleteByPathId(path.getId());
            path.refreshComputedAt();
        }

        path = learningPathRepository.save(path);

        // Load all chapters for subject+grade, then topics
        List<Chapter> chapters = chapterRepository.findBySubjectIdAndGradeId(subjectId, gradeId);
        List<Topic> allTopics = new ArrayList<>();
        for (Chapter chapter : chapters) {
            allTopics.addAll(topicRepository.findByChapterId(chapter.getId()).stream()
                .filter(Topic::isActive).toList());
        }

        if (allTopics.isEmpty()) {
            log.debug("No active topics found for subject {} grade {}", subjectId, gradeId);
            eventPublisher.publishPathComputed(studentId, path.getId(), subjectId);
            return path;
        }

        // Load prerequisites for the subject and sort topics
        List<TopicPrerequisite> prerequisites = topicPrerequisiteRepository.findBySubjectId(subjectId);
        List<Topic> sortedTopics = topologicalSortService.sort(allTopics, prerequisites);

        // Fetch mastery data (map of topicCode -> ratio)
        Map<String, Double> masteryMap = masteryDataPort.fetchMastery(studentId, subjectId, jwt);

        // Determine which chapters are "next" (earliest active chapter)
        UUID nextChapterId = chapters.isEmpty() ? null : chapters.get(0).getId();

        // Build learning path items
        final UUID pathId = path.getId();
        List<LearningPathItem> items = new ArrayList<>();
        for (int i = 0; i < sortedTopics.size(); i++) {
            Topic topic = sortedTopics.get(i);
            double masteryRatio = masteryMap.getOrDefault(topic.getTopicCode(), 0.0);
            boolean isNextChapter = topic.getChapterId().equals(nextChapterId);
            BigDecimal urgencyScore = urgencyScoreCalculator.calculate(topic, masteryRatio, isNextChapter);
            items.add(LearningPathItem.create(pathId, topic.getId(), i + 1, urgencyScore));
        }

        for (LearningPathItem item : items) {
            learningPathItemRepository.save(item);
        }

        eventPublisher.publishPathComputed(studentId, pathId, subjectId);
        return path;
    }

    @Override
    @Transactional
    public LearningPathItem updateItemStatus(UUID itemId, UUID studentId, PathItemStatus newStatus) {
        LearningPathItem item = learningPathItemRepository
            .findByIdAndPathStudentId(itemId, studentId)
            .orElseThrow(() -> new NoSuchElementException("Learning path item not found: " + itemId));

        switch (newStatus) {
            case IN_PROGRESS -> item.markInProgress();
            case COMPLETED -> {
                item.markCompleted();
                eventPublisher.publishPathItemCompleted(studentId, itemId, item.getTopicId());
            }
            case SKIPPED -> item.skip();
            default -> throw new IllegalArgumentException("Cannot set status to: " + newStatus);
        }

        return learningPathItemRepository.save(item);
    }
}
