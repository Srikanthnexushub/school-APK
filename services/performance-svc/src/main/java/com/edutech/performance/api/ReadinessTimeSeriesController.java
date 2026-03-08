package com.edutech.performance.api;

import com.edutech.performance.domain.port.out.ReadinessTimeSeriesPort;
import com.edutech.performance.domain.port.out.ReadinessTimeSeriesPort.DailyReadiness;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * REST endpoint for TimescaleDB-backed time-series readiness trend data.
 * Students may only query their own data; ADMINs may query any student.
 */
@RestController
@RequestMapping("/api/v1/performance/readiness")
public class ReadinessTimeSeriesController {

    private final ReadinessTimeSeriesPort readinessTimeSeriesPort;

    public ReadinessTimeSeriesController(ReadinessTimeSeriesPort readinessTimeSeriesPort) {
        this.readinessTimeSeriesPort = readinessTimeSeriesPort;
    }

    /**
     * GET /api/v1/performance/readiness/{studentId}/trend?days=30
     * Returns daily average ERS scores for the given student over the last N days.
     * Students may only retrieve their own trend; ADMINs may retrieve any student's trend.
     */
    @GetMapping("/{studentId}/trend")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<List<DailyReadiness>> getReadinessTrend(
            @PathVariable String studentId,
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !authentication.getName().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Students may only view their own readiness trend");
        }

        if (days < 1 || days > 365) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "days parameter must be between 1 and 365");
        }

        List<DailyReadiness> trend = readinessTimeSeriesPort.getDailyReadiness(studentId, days);
        return ResponseEntity.ok(trend);
    }
}
