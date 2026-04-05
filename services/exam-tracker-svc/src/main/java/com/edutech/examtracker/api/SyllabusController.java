package com.edutech.examtracker.api;

import com.edutech.examtracker.application.dto.SyllabusModuleResponse;
import com.edutech.examtracker.application.dto.SyllabusProgressResponse;
import com.edutech.examtracker.application.dto.UpdateSyllabusModuleRequest;
import com.edutech.examtracker.application.exception.ExamAccessDeniedException;
import com.edutech.examtracker.domain.port.in.GetSyllabusProgressUseCase;
import com.edutech.examtracker.domain.port.in.UpdateSyllabusModuleUseCase;
import com.edutech.examtracker.infrastructure.security.AuthPrincipal;
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
        AuthPrincipal principal = AuthPrincipal.current();
        validateAccess(principal, studentId);
        SyllabusProgressResponse response = getSyllabusProgressUseCase.getSyllabusProgress(studentId, enrollmentId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/syllabus-modules/{moduleId}")
    public ResponseEntity<SyllabusModuleResponse> updateModule(
            @PathVariable UUID moduleId,
            @Valid @RequestBody UpdateSyllabusModuleRequest request) {
        // Resolve the student who owns this module, then enforce ownership
        AuthPrincipal principal = AuthPrincipal.current();
        UUID moduleStudentId = getSyllabusProgressUseCase.resolveModuleStudentId(moduleId);
        validateAccess(principal, moduleStudentId);
        SyllabusModuleResponse response = updateSyllabusModuleUseCase.updateModule(moduleId, request);
        return ResponseEntity.ok(response);
    }

    // ── Access control ────────────────────────────────────────────────────────

    private void validateAccess(AuthPrincipal principal, UUID studentId) {
        if (principal.isSuperAdmin() || principal.isInstitutionAdmin()) return;
        if (principal.isCenterAdmin()) return;
        if (principal.isTeacher()) return;
        if (principal.isParent()) return;
        if (principal.isStudent() && principal.userId().equals(studentId)) return;
        throw new ExamAccessDeniedException("Access denied to syllabus data for student: " + studentId);
    }
}
