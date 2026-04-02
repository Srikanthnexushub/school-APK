package com.edutech.curricular.application.service;

import com.edutech.curricular.domain.model.Topic;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Computes urgency scores for learning path items.
 *
 * <p>Formula: score = (1.0 - masteryRatio) * bloomsWeight * recencyBoost
 * <ul>
 *   <li>masteryRatio: 0.0-1.0 (0=not started, 1=fully mastered)</li>
 *   <li>bloomsWeight: topic.bloomsLevel.getWeight() (1.0-2.0)</li>
 *   <li>recencyBoost: 1.5 if topic is in the next chapter, 1.0 otherwise</li>
 *   <li>Result is multiplied by 50 and clamped to 0.00-100.00</li>
 * </ul>
 */
@Component
public class UrgencyScoreCalculator {

    public BigDecimal calculate(Topic topic, double masteryRatio, boolean isNextChapter) {
        double raw = (1.0 - masteryRatio) * topic.getBloomsLevel().getWeight() * (isNextChapter ? 1.5 : 1.0);
        double score = Math.min(100.0, raw * 50.0);
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }
}
