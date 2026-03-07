package com.edutech.performance.api;

import com.edutech.performance.application.dto.RecordWeakAreaRequest;
import com.edutech.performance.application.dto.WeakAreaResponse;
import com.edutech.performance.domain.port.in.GetWeakAreasUseCase;
import com.edutech.performance.domain.port.in.RecordWeakAreaUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/performance/students/{studentId}")
public class WeakAreaController {

    private final RecordWeakAreaUseCase recordWeakAreaUseCase;
    private final GetWeakAreasUseCase getWeakAreasUseCase;

    public WeakAreaController(RecordWeakAreaUseCase recordWeakAreaUseCase,
                               GetWeakAreasUseCase getWeakAreasUseCase) {
        this.recordWeakAreaUseCase = recordWeakAreaUseCase;
        this.getWeakAreasUseCase = getWeakAreasUseCase;
    }

    @PostMapping("/weak-areas")
    public ResponseEntity<WeakAreaResponse> recordWeakArea(
            @PathVariable UUID studentId,
            @Valid @RequestBody RecordWeakAreaRequest request) {
        WeakAreaResponse response = recordWeakAreaUseCase.recordWeakArea(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/enrollments/{enrollmentId}/weak-areas")
    public ResponseEntity<List<WeakAreaResponse>> getWeakAreas(
            @PathVariable UUID studentId,
            @PathVariable UUID enrollmentId) {
        List<WeakAreaResponse> weakAreas = getWeakAreasUseCase.getWeakAreas(studentId, enrollmentId);
        return ResponseEntity.ok(weakAreas);
    }

    @GetMapping("/enrollments/{enrollmentId}/weak-areas/top")
    public ResponseEntity<List<WeakAreaResponse>> getTopWeakAreas(
            @PathVariable UUID studentId,
            @PathVariable UUID enrollmentId,
            @RequestParam(defaultValue = "5") int limit) {
        List<WeakAreaResponse> topWeakAreas = getWeakAreasUseCase.getTopWeakAreas(studentId, enrollmentId, limit);
        return ResponseEntity.ok(topWeakAreas);
    }
}
