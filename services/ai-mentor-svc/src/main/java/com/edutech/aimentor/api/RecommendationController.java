package com.edutech.aimentor.api;

import com.edutech.aimentor.application.dto.RecommendationResponse;
import com.edutech.aimentor.domain.port.in.GetRecommendationsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendations", description = "AI-generated learning recommendations")
public class RecommendationController {

    private final GetRecommendationsUseCase getRecommendationsUseCase;

    public RecommendationController(GetRecommendationsUseCase getRecommendationsUseCase) {
        this.getRecommendationsUseCase = getRecommendationsUseCase;
    }

    @GetMapping
    @Operation(summary = "Get active recommendations for the authenticated student")
    public ResponseEntity<Page<RecommendationResponse>> getRecommendations(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        List<RecommendationResponse> all = getRecommendationsUseCase.getRecommendations(userId);
        int start = (int) pageRequest.getOffset();
        int end = Math.min(start + pageRequest.getPageSize(), all.size());
        Page<RecommendationResponse> recommendations = new org.springframework.data.domain.PageImpl<>(
                start < all.size() ? all.subList(start, end) : List.of(), pageRequest, all.size());
        return ResponseEntity.ok(recommendations);
    }
}
