// src/main/java/com/edutech/assess/api/GradeController.java
package com.edutech.assess.api;

import com.edutech.assess.application.dto.AuthPrincipal;
import com.edutech.assess.application.dto.GradeResponse;
import com.edutech.assess.application.service.GradeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping("/api/v1/exams/{examId}/grades")
    public List<GradeResponse> listByExam(
            @PathVariable UUID examId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return gradeService.listGradesByExam(examId, principal);
    }

    @GetMapping("/api/v1/students/{studentId}/grades")
    public List<GradeResponse> listByStudent(
            @PathVariable UUID studentId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return gradeService.listGradesByStudent(studentId, principal);
    }
}
