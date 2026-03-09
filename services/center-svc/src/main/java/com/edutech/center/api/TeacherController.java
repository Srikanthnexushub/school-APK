// src/main/java/com/edutech/center/api/TeacherController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AssignTeacherRequest;
import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.TeacherResponse;
import com.edutech.center.application.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/centers/{centerId}/teachers")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Teachers", description = "Teacher assignment within a center")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign a user as teacher to this center")
    public TeacherResponse assignTeacher(@PathVariable UUID centerId,
                                         @Valid @RequestBody AssignTeacherRequest request,
                                         @AuthenticationPrincipal AuthPrincipal principal) {
        return teacherService.assignTeacher(centerId, request, principal);
    }

    @GetMapping
    @Operation(summary = "List teachers for a center")
    public Page<TeacherResponse> listTeachers(@PathVariable UUID centerId,
                                              @AuthenticationPrincipal AuthPrincipal principal,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        return teacherService.listTeachers(centerId, principal, PageRequest.of(page, size));
    }
}
