package com.edutech.examtracker.api;

import com.edutech.examtracker.application.dto.EnrollInExamRequest;
import com.edutech.examtracker.application.dto.ExamEnrollmentResponse;
import com.edutech.examtracker.domain.port.in.EnrollInExamUseCase;
import com.edutech.examtracker.domain.port.in.GetEnrollmentUseCase;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping("/api/v1/exam-tracker")
public class ExamEnrollmentController {

    private final EnrollInExamUseCase enrollInExamUseCase;
    private final GetEnrollmentUseCase getEnrollmentUseCase;

    public ExamEnrollmentController(EnrollInExamUseCase enrollInExamUseCase,
                                    GetEnrollmentUseCase getEnrollmentUseCase) {
        this.enrollInExamUseCase = enrollInExamUseCase;
        this.getEnrollmentUseCase = getEnrollmentUseCase;
    }

    @PostMapping("/students/{studentId}/enrollments")
    public ResponseEntity<ExamEnrollmentResponse> enroll(
            @PathVariable UUID studentId,
            @Valid @RequestBody EnrollInExamRequest request) {
        ExamEnrollmentResponse response = enrollInExamUseCase.enroll(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/students/{studentId}/enrollments")
    public ResponseEntity<Page<ExamEnrollmentResponse>> getStudentEnrollments(
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        List<ExamEnrollmentResponse> all = getEnrollmentUseCase.getStudentEnrollments(studentId);
        int start = (int) pageRequest.getOffset();
        int end = Math.min(start + pageRequest.getPageSize(), all.size());
        Page<ExamEnrollmentResponse> result = new org.springframework.data.domain.PageImpl<>(
                start < all.size() ? all.subList(start, end) : List.of(), pageRequest, all.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/enrollments/{enrollmentId}")
    public ResponseEntity<ExamEnrollmentResponse> getEnrollment(
            @PathVariable UUID enrollmentId) {
        ExamEnrollmentResponse response = getEnrollmentUseCase.getEnrollment(enrollmentId);
        return ResponseEntity.ok(response);
    }
}
