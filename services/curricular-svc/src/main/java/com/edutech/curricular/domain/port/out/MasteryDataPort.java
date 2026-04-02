package com.edutech.curricular.domain.port.out;

import java.util.Map;
import java.util.UUID;

public interface MasteryDataPort {
    /**
     * Fetch topic mastery ratios for a student in a subject.
     *
     * @return a map of topicCode -> masteryRatio (0.0-1.0)
     */
    Map<String, Double> fetchMastery(UUID studentId, UUID subjectId, String jwt);
}
