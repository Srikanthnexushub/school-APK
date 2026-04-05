package com.edutech.careeroracle.api;

import com.edutech.careeroracle.application.dto.CollegePredictionResponse;
import com.edutech.careeroracle.application.exception.CareerAccessDeniedException;
import com.edutech.careeroracle.domain.port.in.PredictCollegesUseCase;
import com.edutech.careeroracle.infrastructure.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/college-predictions")
@Tag(name = "College Predictions", description = "College admission prediction endpoints")
public class CollegePredictionController {

    private final PredictCollegesUseCase predictCollegesUseCase;

    public CollegePredictionController(PredictCollegesUseCase predictCollegesUseCase) {
        this.predictCollegesUseCase = predictCollegesUseCase;
    }

    @PostMapping("/students/{studentId}/predict")
    @Operation(summary = "Generate college predictions for a student")
    public ResponseEntity<List<CollegePredictionResponse>> predictColleges(@PathVariable UUID studentId) {
        AuthPrincipal principal = AuthPrincipal.current();
        validateAccess(principal, studentId);
        List<CollegePredictionResponse> predictions = predictCollegesUseCase.predictColleges(studentId);
        return ResponseEntity.ok(predictions);
    }

    @GetMapping("/students/{studentId}")
    @Operation(summary = "Get college predictions for a student")
    public ResponseEntity<Page<CollegePredictionResponse>> getPredictions(
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        AuthPrincipal principal = AuthPrincipal.current();
        validateAccess(principal, studentId);
        PageRequest pageRequest = PageRequest.of(page, size);
        List<CollegePredictionResponse> all = predictCollegesUseCase.getPredictions(studentId);
        int start = (int) pageRequest.getOffset();
        int end = Math.min(start + pageRequest.getPageSize(), all.size());
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(
                start < all.size() ? all.subList(start, end) : List.of(), pageRequest, all.size()));
    }

    // ── Access control ────────────────────────────────────────────────────────

    private void validateAccess(AuthPrincipal principal, UUID studentId) {
        if (principal.isSuperAdmin() || principal.isInstitutionAdmin()) return;
        if (principal.isCenterAdmin()) return;
        if (principal.isTeacher()) return;
        if (principal.isParent()) return;
        if (principal.isStudent() && principal.userId().equals(studentId)) return;
        throw new CareerAccessDeniedException("Access denied to college predictions for student: " + studentId);
    }
}
