package com.edutech.student.api;

import com.edutech.student.application.dto.SetTargetExamRequest;
import com.edutech.student.application.dto.TargetExamResponse;
import com.edutech.student.application.service.TargetExamService;
import com.edutech.student.domain.port.in.SetTargetExamUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students/{studentId}/target-exams")
public class TargetExamController {

    private final SetTargetExamUseCase setTargetExamUseCase;
    private final TargetExamService targetExamService;

    public TargetExamController(SetTargetExamUseCase setTargetExamUseCase,
                                 TargetExamService targetExamService) {
        this.setTargetExamUseCase = setTargetExamUseCase;
        this.targetExamService = targetExamService;
    }

    @PostMapping
    public ResponseEntity<TargetExamResponse> setTargetExam(
            @PathVariable UUID studentId,
            @Valid @RequestBody SetTargetExamRequest request) {
        TargetExamResponse response = setTargetExamUseCase.setTargetExam(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TargetExamResponse>> getTargetExams(@PathVariable UUID studentId) {
        return ResponseEntity.ok(targetExamService.getExamsByStudentId(studentId));
    }

    @DeleteMapping("/{examId}")
    public ResponseEntity<Void> deleteTargetExam(
            @PathVariable UUID studentId,
            @PathVariable UUID examId) {
        targetExamService.softDeleteExam(studentId, examId);
        return ResponseEntity.noContent().build();
    }
}
