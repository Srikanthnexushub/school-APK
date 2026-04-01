package com.edutech.chat.infrastructure.adapter.out.webclient;

import java.util.List;

public record PerformanceDto(
    Double ersScore,
    List<WeakAreaItem> weakAreas,
    List<MasteryItem> subjectMastery
) {
    public static PerformanceDto empty() {
        return new PerformanceDto(null, List.of(), List.of());
    }
}
