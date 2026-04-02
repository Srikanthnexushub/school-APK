package com.edutech.curricular.service;

import com.edutech.curricular.application.service.UrgencyScoreCalculator;
import com.edutech.curricular.domain.model.Topic;
import com.edutech.curricular.domain.model.enums.BloomsLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UrgencyScoreCalculator tests")
class UrgencyScoreCalculatorTest {

    private UrgencyScoreCalculator calculator;

    // Immutable static fixtures
    static final UUID CHAPTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    static Topic buildTopic(BloomsLevel level) {
        Topic t = new Topic();
        t.setId(UUID.randomUUID());
        t.setChapterId(CHAPTER_ID);
        t.setTopicCode("T-X");
        t.setTopicTitle("Test Topic");
        t.setBloomsLevel(level);
        t.setEstHours(BigDecimal.ONE);
        t.setSortOrder(0);
        t.setActive(true);
        return t;
    }

    @BeforeEach
    void setUp() {
        calculator = new UrgencyScoreCalculator();
    }

    @Test
    @DisplayName("masteryRatio=0.0, REMEMBER, not next chapter -> score = 50.00")
    void not_mastered_remember_not_next() {
        // (1.0 - 0.0) * 1.0 * 1.0 * 50 = 50.00
        BigDecimal score = calculator.calculate(buildTopic(BloomsLevel.REMEMBER), 0.0, false);
        assertThat(score).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("masteryRatio=1.0, any level -> score = 0.00")
    void fully_mastered_returns_zero() {
        BigDecimal score = calculator.calculate(buildTopic(BloomsLevel.CREATE), 1.0, true);
        assertThat(score).isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
    }

    @Test
    @DisplayName("masteryRatio=0.0, CREATE (weight 2.0), next chapter -> clamped to 100.00")
    void not_mastered_create_next_chapter_clamped() {
        // (1.0 - 0.0) * 2.0 * 1.5 * 50 = 150 -> clamped to 100.00
        BigDecimal score = calculator.calculate(buildTopic(BloomsLevel.CREATE), 0.0, true);
        assertThat(score).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("masteryRatio=0.5, APPLY (weight 1.4), not next chapter -> score = 35.00")
    void half_mastered_apply_not_next() {
        // (1.0 - 0.5) * 1.4 * 1.0 * 50 = 35.00
        BigDecimal score = calculator.calculate(buildTopic(BloomsLevel.APPLY), 0.5, false);
        assertThat(score).isEqualByComparingTo(new BigDecimal("35.00"));
    }
}
