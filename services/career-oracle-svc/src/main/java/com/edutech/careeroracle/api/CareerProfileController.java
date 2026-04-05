package com.edutech.careeroracle.api;

import com.edutech.careeroracle.application.dto.CareerProfileResponse;
import com.edutech.careeroracle.application.dto.CreateCareerProfileRequest;
import com.edutech.careeroracle.application.dto.UpdateCareerProfileRequest;
import com.edutech.careeroracle.application.exception.CareerAccessDeniedException;
import com.edutech.careeroracle.domain.port.in.CreateCareerProfileUseCase;
import com.edutech.careeroracle.domain.port.in.GetCareerProfileUseCase;
import com.edutech.careeroracle.domain.port.in.UpdateCareerProfileUseCase;
import com.edutech.careeroracle.infrastructure.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/career-profiles")
@Tag(name = "Career Profiles", description = "Career profile management endpoints")
public class CareerProfileController {

    private final CreateCareerProfileUseCase createCareerProfileUseCase;
    private final GetCareerProfileUseCase getCareerProfileUseCase;
    private final UpdateCareerProfileUseCase updateCareerProfileUseCase;

    public CareerProfileController(CreateCareerProfileUseCase createCareerProfileUseCase,
                                    GetCareerProfileUseCase getCareerProfileUseCase,
                                    UpdateCareerProfileUseCase updateCareerProfileUseCase) {
        this.createCareerProfileUseCase = createCareerProfileUseCase;
        this.getCareerProfileUseCase = getCareerProfileUseCase;
        this.updateCareerProfileUseCase = updateCareerProfileUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new career profile")
    public ResponseEntity<CareerProfileResponse> createCareerProfile(
            @Valid @RequestBody CreateCareerProfileRequest request) {
        AuthPrincipal principal = AuthPrincipal.current();
        validateAccess(principal, request.studentId());
        CareerProfileResponse response = createCareerProfileUseCase.createCareerProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{profileId}")
    @Operation(summary = "Get career profile by ID")
    public ResponseEntity<CareerProfileResponse> getCareerProfileById(@PathVariable UUID profileId) {
        // Profile ID lookup — resolve ownership after fetch; admins/teachers/parents are allowed
        AuthPrincipal principal = AuthPrincipal.current();
        CareerProfileResponse response = getCareerProfileUseCase.getCareerProfileById(profileId);
        validateAccess(principal, response.studentId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-student/{studentId}")
    @Operation(summary = "Get career profile by student ID")
    public ResponseEntity<CareerProfileResponse> getCareerProfileByStudentId(@PathVariable UUID studentId) {
        AuthPrincipal principal = AuthPrincipal.current();
        validateAccess(principal, studentId);
        CareerProfileResponse response = getCareerProfileUseCase.getCareerProfileByStudentId(studentId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{profileId}")
    @Operation(summary = "Update a career profile")
    public ResponseEntity<CareerProfileResponse> updateCareerProfile(
            @PathVariable UUID profileId,
            @Valid @RequestBody UpdateCareerProfileRequest request) {
        // Resolve ownership after fetch (profile belongs to a student)
        AuthPrincipal principal = AuthPrincipal.current();
        CareerProfileResponse existing = getCareerProfileUseCase.getCareerProfileById(profileId);
        validateAccess(principal, existing.studentId());
        CareerProfileResponse response = updateCareerProfileUseCase.updateCareerProfile(profileId, request);
        return ResponseEntity.ok(response);
    }

    // ── Access control ────────────────────────────────────────────────────────

    /**
     * Validates that the calling principal is allowed to access the given student's career data.
     * Rules:
     *  - SUPER_ADMIN / INSTITUTION_ADMIN: always allowed
     *  - CENTER_ADMIN: allowed (manages their center's students)
     *  - TEACHER: allowed (views their students)
     *  - PARENT: allowed (linked student validated at gateway)
     *  - STUDENT: only their own data
     */
    private void validateAccess(AuthPrincipal principal, UUID studentId) {
        if (principal.isSuperAdmin() || principal.isInstitutionAdmin()) return;
        if (principal.isCenterAdmin()) return;
        if (principal.isTeacher()) return;
        if (principal.isParent()) return;
        if (principal.isStudent() && principal.userId().equals(studentId)) return;
        throw new CareerAccessDeniedException("Access denied to career data for student: " + studentId);
    }
}
