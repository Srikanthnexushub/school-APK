package com.edutech.performance.api;

import com.edutech.performance.application.dto.PerformanceDashboardResponse;
import com.edutech.performance.domain.port.in.GetPerformanceDashboardUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/performance/students/{studentId}/enrollments/{enrollmentId}/dashboard")
public class PerformanceDashboardController {

    private final GetPerformanceDashboardUseCase getPerformanceDashboardUseCase;

    public PerformanceDashboardController(GetPerformanceDashboardUseCase getPerformanceDashboardUseCase) {
        this.getPerformanceDashboardUseCase = getPerformanceDashboardUseCase;
    }

    @GetMapping
    public ResponseEntity<PerformanceDashboardResponse> getDashboard(
            @PathVariable UUID studentId,
            @PathVariable UUID enrollmentId) {
        PerformanceDashboardResponse response = getPerformanceDashboardUseCase.getDashboard(studentId, enrollmentId);
        return ResponseEntity.ok(response);
    }
}
