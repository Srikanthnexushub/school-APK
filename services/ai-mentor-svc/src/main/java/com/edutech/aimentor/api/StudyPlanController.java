package com.edutech.aimentor.api;

import com.edutech.aimentor.application.dto.CreateStudyPlanRequest;
import com.edutech.aimentor.application.dto.StudyPlanItemResponse;
import com.edutech.aimentor.application.dto.StudyPlanResponse;
import com.edutech.aimentor.domain.port.in.CreateStudyPlanUseCase;
import com.edutech.aimentor.domain.port.in.GetStudyPlanUseCase;
import com.edutech.aimentor.domain.port.in.ListStudyPlansUseCase;
import com.edutech.aimentor.domain.port.in.UpdateStudyPlanItemUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/study-plans")
@Tag(name = "Study Plans", description = "AI-powered study plan management with SM-2 spaced repetition")
public class StudyPlanController {

    private final CreateStudyPlanUseCase createStudyPlanUseCase;
    private final GetStudyPlanUseCase getStudyPlanUseCase;
    private final ListStudyPlansUseCase listStudyPlansUseCase;
    private final UpdateStudyPlanItemUseCase updateStudyPlanItemUseCase;

    public StudyPlanController(CreateStudyPlanUseCase createStudyPlanUseCase,
                               GetStudyPlanUseCase getStudyPlanUseCase,
                               ListStudyPlansUseCase listStudyPlansUseCase,
                               UpdateStudyPlanItemUseCase updateStudyPlanItemUseCase) {
        this.createStudyPlanUseCase = createStudyPlanUseCase;
        this.getStudyPlanUseCase = getStudyPlanUseCase;
        this.listStudyPlansUseCase = listStudyPlansUseCase;
        this.updateStudyPlanItemUseCase = updateStudyPlanItemUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new study plan")
    public ResponseEntity<StudyPlanResponse> createStudyPlan(
            @Valid @RequestBody CreateStudyPlanRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole) {
        // Always bind studentId from the authenticated user (header-injected by gateway)
        CreateStudyPlanRequest secured = new CreateStudyPlanRequest(
                userId,
                request.enrollmentId(),
                request.title(),
                request.description(),
                request.targetExamDate(),
                request.items()
        );
        StudyPlanResponse response = createStudyPlanUseCase.createStudyPlan(secured);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all study plans for the authenticated student")
    public ResponseEntity<Page<StudyPlanResponse>> listStudyPlans(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        List<StudyPlanResponse> all = listStudyPlansUseCase.listStudyPlans(userId);
        int start = (int) pageRequest.getOffset();
        int end = Math.min(start + pageRequest.getPageSize(), all.size());
        Page<StudyPlanResponse> response = new org.springframework.data.domain.PageImpl<>(
                start < all.size() ? all.subList(start, end) : List.of(), pageRequest, all.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Get a study plan by its ID")
    public ResponseEntity<StudyPlanResponse> getStudyPlanById(
            @PathVariable UUID planId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole) {
        StudyPlanResponse response = getStudyPlanUseCase.getStudyPlanById(planId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-enrollment")
    @Operation(summary = "Get study plan for a specific student enrollment")
    public ResponseEntity<StudyPlanResponse> getStudyPlan(
            @RequestParam UUID studentId,
            @RequestParam UUID enrollmentId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole) {
        StudyPlanResponse response = getStudyPlanUseCase.getStudyPlan(studentId, enrollmentId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{itemId}/review")
    @Operation(summary = "Review a study plan item and apply SM-2 algorithm update")
    public ResponseEntity<StudyPlanItemResponse> reviewItem(
            @PathVariable UUID itemId,
            @RequestParam @Min(0) @Max(5) int quality,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole) {
        StudyPlanItemResponse response = updateStudyPlanItemUseCase.reviewItem(itemId, userId, quality);
        return ResponseEntity.ok(response);
    }
}
