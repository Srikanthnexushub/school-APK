package com.edutech.curricular.service;

import com.edutech.curricular.application.service.LearningPathService;
import com.edutech.curricular.application.service.TopologicalSortService;
import com.edutech.curricular.application.service.UrgencyScoreCalculator;
import com.edutech.curricular.domain.model.Chapter;
import com.edutech.curricular.domain.model.LearningPath;
import com.edutech.curricular.domain.model.LearningPathItem;
import com.edutech.curricular.domain.model.Topic;
import com.edutech.curricular.domain.model.enums.BloomsLevel;
import com.edutech.curricular.domain.model.enums.PathItemStatus;
import com.edutech.curricular.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LearningPathService tests")
class LearningPathServiceTest {

    @Mock private LearningPathRepository learningPathRepository;
    @Mock private LearningPathItemRepository learningPathItemRepository;
    @Mock private ChapterRepository chapterRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private TopicPrerequisiteRepository topicPrerequisiteRepository;
    @Mock private MasteryDataPort masteryDataPort;
    @Mock private CurricularEventPublisher eventPublisher;

    // Real implementations
    private TopologicalSortService topologicalSortService;
    private UrgencyScoreCalculator urgencyScoreCalculator;
    private LearningPathService service;

    // Static immutable fixtures
    static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID SUBJECT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID GRADE_ID   = UUID.fromString("33333333-3333-3333-3333-333333333333");
    static final UUID CHAPTER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    static final UUID PATH_ID    = UUID.fromString("55555555-5555-5555-5555-555555555555");
    static final UUID ITEM_ID    = UUID.fromString("66666666-6666-6666-6666-666666666666");
    static final UUID TOPIC_1_ID = UUID.fromString("77777777-7777-7777-7777-000000000001");
    static final UUID TOPIC_2_ID = UUID.fromString("77777777-7777-7777-7777-000000000002");
    static final UUID TOPIC_3_ID = UUID.fromString("77777777-7777-7777-7777-000000000003");

    static Topic buildTopic(UUID id, String code) {
        Topic t = new Topic();
        t.setId(id);
        t.setChapterId(CHAPTER_ID);
        t.setTopicCode(code);
        t.setTopicTitle("Topic " + code);
        t.setBloomsLevel(BloomsLevel.REMEMBER);
        t.setEstHours(BigDecimal.ONE);
        t.setSortOrder(0);
        t.setActive(true);
        return t;
    }

    @BeforeEach
    void setUp() {
        topologicalSortService = new TopologicalSortService();
        urgencyScoreCalculator = new UrgencyScoreCalculator();
        service = new LearningPathService(
            learningPathRepository, learningPathItemRepository,
            chapterRepository, topicRepository, topicPrerequisiteRepository,
            masteryDataPort, topologicalSortService, urgencyScoreCalculator, eventPublisher
        );
    }

    @Test
    @DisplayName("computeOrRefresh: no existing path, 3 topics, no prerequisites -> creates path with 3 items")
    void compute_creates_path_with_3_items() {
        Chapter chapter = new Chapter();
        chapter.setId(CHAPTER_ID);
        chapter.setSubjectId(SUBJECT_ID);
        chapter.setGradeId(GRADE_ID);
        chapter.setActive(true);

        Topic t1 = buildTopic(TOPIC_1_ID, "T1");
        Topic t2 = buildTopic(TOPIC_2_ID, "T2");
        Topic t3 = buildTopic(TOPIC_3_ID, "T3");

        LearningPath savedPath = LearningPath.create(STUDENT_ID, SUBJECT_ID, GRADE_ID);
        savedPath.setId(PATH_ID);

        when(learningPathRepository.findByStudentIdAndSubjectIdAndGradeId(STUDENT_ID, SUBJECT_ID, GRADE_ID))
            .thenReturn(Optional.empty());
        when(learningPathRepository.save(any())).thenReturn(savedPath);
        when(chapterRepository.findBySubjectIdAndGradeId(SUBJECT_ID, GRADE_ID)).thenReturn(List.of(chapter));
        when(topicRepository.findByChapterId(CHAPTER_ID)).thenReturn(List.of(t1, t2, t3));
        when(topicPrerequisiteRepository.findBySubjectId(SUBJECT_ID)).thenReturn(List.of());
        when(masteryDataPort.fetchMastery(STUDENT_ID, SUBJECT_ID, "test-jwt")).thenReturn(Map.of());
        when(learningPathItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LearningPath result = service.computeOrRefresh(STUDENT_ID, SUBJECT_ID, GRADE_ID, "test-jwt");

        assertThat(result).isNotNull();
        assertThat(result.getStudentId()).isEqualTo(STUDENT_ID);
        verify(learningPathItemRepository, times(3)).save(any());
        verify(eventPublisher).publishPathComputed(eq(STUDENT_ID), eq(PATH_ID), eq(SUBJECT_ID));
    }

    @Test
    @DisplayName("computeOrRefresh: existing path found -> updates computedAt and replaces items")
    void compute_existing_path_updates() {
        Chapter chapter = new Chapter();
        chapter.setId(CHAPTER_ID);
        chapter.setSubjectId(SUBJECT_ID);
        chapter.setGradeId(GRADE_ID);
        chapter.setActive(true);

        Topic t1 = buildTopic(TOPIC_1_ID, "T1");

        LearningPath existingPath = LearningPath.create(STUDENT_ID, SUBJECT_ID, GRADE_ID);
        existingPath.setId(PATH_ID);

        when(learningPathRepository.findByStudentIdAndSubjectIdAndGradeId(STUDENT_ID, SUBJECT_ID, GRADE_ID))
            .thenReturn(Optional.of(existingPath));
        when(learningPathRepository.save(any())).thenReturn(existingPath);
        when(chapterRepository.findBySubjectIdAndGradeId(SUBJECT_ID, GRADE_ID)).thenReturn(List.of(chapter));
        when(topicRepository.findByChapterId(CHAPTER_ID)).thenReturn(List.of(t1));
        when(topicPrerequisiteRepository.findBySubjectId(SUBJECT_ID)).thenReturn(List.of());
        when(masteryDataPort.fetchMastery(STUDENT_ID, SUBJECT_ID, "jwt")).thenReturn(Map.of());
        when(learningPathItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LearningPath result = service.computeOrRefresh(STUDENT_ID, SUBJECT_ID, GRADE_ID, "jwt");

        assertThat(result).isNotNull();
        verify(learningPathItemRepository).deleteByPathId(PATH_ID);
        verify(learningPathItemRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("updateItemStatus: PENDING -> COMPLETED calls markCompleted and publishes event")
    void update_item_to_completed_publishes_event() {
        LearningPathItem item = LearningPathItem.create(PATH_ID, TOPIC_1_ID, 1, BigDecimal.TEN);
        item.setId(ITEM_ID);

        when(learningPathItemRepository.findByIdAndPathStudentId(ITEM_ID, STUDENT_ID))
            .thenReturn(Optional.of(item));
        when(learningPathItemRepository.save(any())).thenReturn(item);

        LearningPathItem result = service.updateItemStatus(ITEM_ID, STUDENT_ID, PathItemStatus.COMPLETED);

        assertThat(result.getStatus()).isEqualTo(PathItemStatus.COMPLETED);
        verify(eventPublisher).publishPathItemCompleted(STUDENT_ID, ITEM_ID, TOPIC_1_ID);
    }

    @Test
    @DisplayName("updateItemStatus: item not found -> throws NoSuchElementException")
    void update_item_not_found_throws() {
        when(learningPathItemRepository.findByIdAndPathStudentId(ITEM_ID, STUDENT_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateItemStatus(ITEM_ID, STUDENT_ID, PathItemStatus.COMPLETED))
            .isInstanceOf(NoSuchElementException.class);
    }
}
