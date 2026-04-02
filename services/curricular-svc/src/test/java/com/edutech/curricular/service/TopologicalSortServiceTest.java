package com.edutech.curricular.service;

import com.edutech.curricular.application.service.CircularDependencyException;
import com.edutech.curricular.application.service.TopologicalSortService;
import com.edutech.curricular.domain.model.Topic;
import com.edutech.curricular.domain.model.TopicPrerequisite;
import com.edutech.curricular.domain.model.enums.BloomsLevel;
import com.edutech.curricular.domain.model.enums.PrerequisiteStrength;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TopologicalSortService tests")
class TopologicalSortServiceTest {

    private TopologicalSortService service;

    // Immutable static fixtures
    static final UUID CHAPTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final UUID TOPIC_A_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    static final UUID TOPIC_B_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
    static final UUID TOPIC_C_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");
    static final UUID TOPIC_D_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004");
    static final UUID CREATED_BY = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

    static final Topic TOPIC_A = buildTopic(TOPIC_A_ID, "T-A", "Topic A");
    static final Topic TOPIC_B = buildTopic(TOPIC_B_ID, "T-B", "Topic B");
    static final Topic TOPIC_C = buildTopic(TOPIC_C_ID, "T-C", "Topic C");
    static final Topic TOPIC_D = buildTopic(TOPIC_D_ID, "T-D", "Topic D");

    static Topic buildTopic(UUID id, String code, String title) {
        Topic t = new Topic();
        t.setId(id);
        t.setChapterId(CHAPTER_ID);
        t.setTopicCode(code);
        t.setTopicTitle(title);
        t.setBloomsLevel(BloomsLevel.REMEMBER);
        t.setEstHours(BigDecimal.ONE);
        t.setSortOrder(0);
        t.setActive(true);
        return t;
    }

    static TopicPrerequisite buildPrereq(UUID fromId, UUID toId) {
        return TopicPrerequisite.create(fromId, toId, PrerequisiteStrength.REQUIRED, CREATED_BY);
    }

    @BeforeEach
    void setUp() {
        service = new TopologicalSortService();
    }

    @Test
    @DisplayName("Empty topic list returns empty result")
    void empty_list_returns_empty() {
        List<Topic> result = service.sort(List.of(), List.of());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Topics with no prerequisites can be returned in any order")
    void no_prerequisites_returns_all_topics() {
        List<Topic> result = service.sort(List.of(TOPIC_A, TOPIC_B, TOPIC_C), List.of());
        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrder(TOPIC_A, TOPIC_B, TOPIC_C);
    }

    @Test
    @DisplayName("Linear chain A -> B -> C: A first, C last")
    void linear_chain_a_b_c() {
        // A must be done before B; B must be done before C
        List<TopicPrerequisite> prereqs = List.of(
            buildPrereq(TOPIC_A_ID, TOPIC_B_ID),
            buildPrereq(TOPIC_B_ID, TOPIC_C_ID)
        );
        List<Topic> result = service.sort(List.of(TOPIC_C, TOPIC_B, TOPIC_A), prereqs);
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(TOPIC_A_ID);
        assertThat(result.get(2).getId()).isEqualTo(TOPIC_C_ID);
        assertThat(result.get(1).getId()).isEqualTo(TOPIC_B_ID);
    }

    @Test
    @DisplayName("Diamond dependency: A -> B, A -> C, B -> D, C -> D: A first, D last")
    void diamond_dependency() {
        // A before B and C; B and C before D
        List<TopicPrerequisite> prereqs = List.of(
            buildPrereq(TOPIC_A_ID, TOPIC_B_ID),
            buildPrereq(TOPIC_A_ID, TOPIC_C_ID),
            buildPrereq(TOPIC_B_ID, TOPIC_D_ID),
            buildPrereq(TOPIC_C_ID, TOPIC_D_ID)
        );
        List<Topic> result = service.sort(List.of(TOPIC_D, TOPIC_C, TOPIC_B, TOPIC_A), prereqs);
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getId()).isEqualTo(TOPIC_A_ID);
        assertThat(result.get(3).getId()).isEqualTo(TOPIC_D_ID);
        // B and C must be in positions 1 and 2 (any order)
        assertThat(result.subList(1, 3)).extracting(Topic::getId)
            .containsExactlyInAnyOrder(TOPIC_B_ID, TOPIC_C_ID);
    }

    @Test
    @DisplayName("Cycle A -> B -> A throws CircularDependencyException")
    void cycle_throws_exception() {
        List<TopicPrerequisite> prereqs = List.of(
            buildPrereq(TOPIC_A_ID, TOPIC_B_ID),
            buildPrereq(TOPIC_B_ID, TOPIC_A_ID)
        );
        assertThatThrownBy(() -> service.sort(List.of(TOPIC_A, TOPIC_B), prereqs))
            .isInstanceOf(CircularDependencyException.class);
    }
}
