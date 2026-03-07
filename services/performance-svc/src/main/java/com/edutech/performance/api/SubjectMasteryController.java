package com.edutech.performance.api;

import com.edutech.performance.application.dto.SubjectMasteryResponse;
import com.edutech.performance.domain.port.in.GetSubjectMasteryUseCase;
import com.edutech.performance.domain.port.in.UpdateSubjectMasteryUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/performance/students/{studentId}/enrollments/{enrollmentId}/subject-mastery")
public class SubjectMasteryController {

    private final UpdateSubjectMasteryUseCase updateSubjectMasteryUseCase;
    private final GetSubjectMasteryUseCase getSubjectMasteryUseCase;

    public SubjectMasteryController(UpdateSubjectMasteryUseCase updateSubjectMasteryUseCase,
                                     GetSubjectMasteryUseCase getSubjectMasteryUseCase) {
        this.updateSubjectMasteryUseCase = updateSubjectMasteryUseCase;
        this.getSubjectMasteryUseCase = getSubjectMasteryUseCase;
    }

    @PutMapping("/{subject}")
    public ResponseEntity<SubjectMasteryResponse> updateMastery(
            @PathVariable UUID studentId,
            @PathVariable UUID enrollmentId,
            @PathVariable String subject,
            @RequestParam BigDecimal masteryPercent) {
        SubjectMasteryResponse response = updateSubjectMasteryUseCase.updateMastery(
                studentId, enrollmentId, subject, masteryPercent);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<SubjectMasteryResponse>> getSubjectMastery(
            @PathVariable UUID studentId,
            @PathVariable UUID enrollmentId) {
        List<SubjectMasteryResponse> masteries = getSubjectMasteryUseCase.getSubjectMastery(studentId, enrollmentId);
        return ResponseEntity.ok(masteries);
    }
}
