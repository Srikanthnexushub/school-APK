package com.edutech.careeroracle.api;

import com.edutech.careeroracle.application.dto.CareerRecommendationResponse;
import com.edutech.careeroracle.domain.port.in.GenerateCareerRecommendationsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/career-recommendations")
@Tag(name = "Career Recommendations", description = "Career recommendation generation and retrieval")
public class CareerRecommendationController {

    private final GenerateCareerRecommendationsUseCase generateCareerRecommendationsUseCase;

    public CareerRecommendationController(GenerateCareerRecommendationsUseCase generateCareerRecommendationsUseCase) {
        this.generateCareerRecommendationsUseCase = generateCareerRecommendationsUseCase;
    }

    @PostMapping("/students/{studentId}/generate")
    @Operation(summary = "Generate career recommendations for a student")
    public ResponseEntity<List<CareerRecommendationResponse>> generateRecommendations(
            @PathVariable UUID studentId,
            @RequestBody(required = false) Map<String, BigDecimal> subjectStrengths) {
        Map<String, BigDecimal> strengths = subjectStrengths != null ? subjectStrengths : Map.of();
        List<CareerRecommendationResponse> recommendations =
                generateCareerRecommendationsUseCase.generateRecommendations(studentId, strengths);
        return ResponseEntity.ok(recommendations);
    }

    @GetMapping("/students/{studentId}")
    @Operation(summary = "Get active career recommendations for a student")
    public ResponseEntity<List<CareerRecommendationResponse>> getActiveRecommendations(
            @PathVariable UUID studentId) {
        List<CareerRecommendationResponse> recommendations =
                generateCareerRecommendationsUseCase.getActiveRecommendations(studentId);
        return ResponseEntity.ok(recommendations);
    }
}
