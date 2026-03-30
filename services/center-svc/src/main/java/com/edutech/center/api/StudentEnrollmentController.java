// src/main/java/com/edutech/center/api/StudentEnrollmentController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.domain.model.BatchMember;
import com.edutech.center.domain.port.out.BatchMemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/centers")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Student Enrollment", description = "Student-facing batch enrollment lookup")
public class StudentEnrollmentController {

    private final BatchMemberRepository memberRepository;

    public StudentEnrollmentController(BatchMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public record EnrollmentResponse(UUID centerId, UUID batchId) {}

    @GetMapping("/student-enrollment/me")
    @Operation(summary = "Get the calling student's active batch enrollment (centerId + batchId)")
    public ResponseEntity<EnrollmentResponse> getMyEnrollment(
            @AuthenticationPrincipal AuthPrincipal principal) {

        return memberRepository.findByStudentId(principal.userId()).stream()
                .filter(BatchMember::isActive)
                .findFirst()
                .map(m -> ResponseEntity.ok(new EnrollmentResponse(m.getCenterId(), m.getBatchId())))
                .orElse(ResponseEntity.noContent().build());
    }
}
