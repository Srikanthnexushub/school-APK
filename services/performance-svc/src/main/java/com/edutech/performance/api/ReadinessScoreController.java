package com.edutech.performance.api;

import com.edutech.performance.application.dto.ReadinessScoreResponse;
import com.edutech.performance.domain.port.in.ComputeReadinessScoreUseCase;
import com.edutech.performance.domain.port.in.GetReadinessScoreUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/performance/students/{studentId}/enrollments/{enrollmentId}/readiness-score")
public class ReadinessScoreController {

    private final ComputeReadinessScoreUseCase computeReadinessScoreUseCase;
    private final GetReadinessScoreUseCase getReadinessScoreUseCase;

    public ReadinessScoreController(ComputeReadinessScoreUseCase computeReadinessScoreUseCase,
                                     GetReadinessScoreUseCase getReadinessScoreUseCase) {
        this.computeReadinessScoreUseCase = computeReadinessScoreUseCase;
        this.getReadinessScoreUseCase = getReadinessScoreUseCase;
    }

    @PostMapping("/compute")
    public ResponseEntity<ReadinessScoreResponse> computeScore(
            @PathVariable UUID studentId,
            @PathVariable UUID enrollmentId) {
        ReadinessScoreResponse response = computeReadinessScoreUseCase.computeScore(studentId, enrollmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ReadinessScoreResponse> getLatestScore(
            @PathVariable UUID studentId,
            @PathVariable UUID enrollmentId) {
        ReadinessScoreResponse response = getReadinessScoreUseCase.getLatestScore(studentId, enrollmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ReadinessScoreResponse>> getScoreHistory(
            @PathVariable UUID studentId,
            @PathVariable UUID enrollmentId) {
        List<ReadinessScoreResponse> history = getReadinessScoreUseCase.getScoreHistory(studentId, enrollmentId);
        return ResponseEntity.ok(history);
    }
}
