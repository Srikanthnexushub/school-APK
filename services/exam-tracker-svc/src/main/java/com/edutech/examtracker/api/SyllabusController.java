package com.edutech.examtracker.api;

import com.edutech.examtracker.application.dto.SyllabusModuleResponse;
import com.edutech.examtracker.application.dto.SyllabusProgressResponse;
import com.edutech.examtracker.application.dto.UpdateSyllabusModuleRequest;
import com.edutech.examtracker.domain.port.in.GetSyllabusProgressUseCase;
import com.edutech.examtracker.domain.port.in.UpdateSyllabusModuleUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exam-tracker")
public class SyllabusController {

    private final GetSyllabusProgressUseCase getSyllabusProgressUseCase;
    private final UpdateSyllabusModuleUseCase updateSyllabusModuleUseCase;

    public SyllabusController(GetSyllabusProgressUseCase getSyllabusProgressUseCase,
                              UpdateSyllabusModuleUseCase updateSyllabusModuleUseCase) {
        this.getSyllabusProgressUseCase = getSyllabusProgressUseCase;
        this.updateSyllabusModuleUseCase = updateSyllabusModuleUseCase;
    }

    @GetMapping("/enrollments/{enrollmentId}/syllabus")
    public ResponseEntity<SyllabusProgressResponse> getSyllabusProgress(
            @PathVariable UUID enrollmentId,
            @RequestParam UUID studentId) {
        SyllabusProgressResponse response = getSyllabusProgressUseCase.getSyllabusProgress(studentId, enrollmentId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/syllabus-modules/{moduleId}")
    public ResponseEntity<SyllabusModuleResponse> updateModule(
            @PathVariable UUID moduleId,
            @Valid @RequestBody UpdateSyllabusModuleRequest request) {
        SyllabusModuleResponse response = updateSyllabusModuleUseCase.updateModule(moduleId, request);
        return ResponseEntity.ok(response);
    }
}
