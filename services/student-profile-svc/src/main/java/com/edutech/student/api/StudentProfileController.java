package com.edutech.student.api;

import com.edutech.student.application.dto.CreateStudentProfileRequest;
import com.edutech.student.application.dto.GenerateLinkOtpRequest;
import com.edutech.student.application.dto.GenerateLinkOtpResponse;
import com.edutech.student.application.dto.PendingLinkResponse;
import com.edutech.student.application.dto.StudentDashboardResponse;
import com.edutech.student.application.dto.StudentLookupResponse;
import com.edutech.student.application.dto.StudentProfileResponse;
import com.edutech.student.application.dto.UpdateStudentProfileRequest;
import com.edutech.student.application.dto.VerifyLinkOtpRequest;
import com.edutech.student.application.dto.VerifyLinkOtpResponse;
import com.edutech.student.application.service.StudentProfileService;
import com.edutech.student.domain.port.in.CreateStudentProfileUseCase;
import com.edutech.student.domain.port.in.GetStudentDashboardUseCase;
import com.edutech.student.domain.port.in.GetStudentProfileUseCase;
import com.edutech.student.domain.port.in.UpdateStudentProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
public class StudentProfileController {

    private final CreateStudentProfileUseCase createStudentProfileUseCase;
    private final GetStudentProfileUseCase getStudentProfileUseCase;
    private final UpdateStudentProfileUseCase updateStudentProfileUseCase;
    private final GetStudentDashboardUseCase getStudentDashboardUseCase;
    private final StudentProfileService studentProfileService;

    public StudentProfileController(CreateStudentProfileUseCase createStudentProfileUseCase,
                                     GetStudentProfileUseCase getStudentProfileUseCase,
                                     UpdateStudentProfileUseCase updateStudentProfileUseCase,
                                     GetStudentDashboardUseCase getStudentDashboardUseCase,
                                     StudentProfileService studentProfileService) {
        this.createStudentProfileUseCase = createStudentProfileUseCase;
        this.getStudentProfileUseCase = getStudentProfileUseCase;
        this.updateStudentProfileUseCase = updateStudentProfileUseCase;
        this.getStudentDashboardUseCase = getStudentDashboardUseCase;
        this.studentProfileService = studentProfileService;
    }

    @PostMapping
    public ResponseEntity<StudentProfileResponse> createProfile(
            @Valid @RequestBody CreateStudentProfileRequest request) {
        StudentProfileResponse response = createStudentProfileUseCase.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<StudentProfileResponse> getMyProfile(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(getStudentProfileUseCase.getProfileByUserId(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentProfileResponse> getProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(getStudentProfileUseCase.getProfile(id));
    }

    @PatchMapping("/me")
    public ResponseEntity<StudentProfileResponse> updateMyProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody UpdateStudentProfileRequest request) {
        StudentProfileResponse profile = getStudentProfileUseCase.getProfileByUserId(userId);
        return ResponseEntity.ok(updateStudentProfileUseCase.updateProfile(profile.id(), request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<StudentProfileResponse> updateProfile(
            @PathVariable UUID id,
            @RequestBody UpdateStudentProfileRequest request) {
        return ResponseEntity.ok(updateStudentProfileUseCase.updateProfile(id, request));
    }

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<StudentDashboardResponse> getDashboard(@PathVariable UUID id) {
        return ResponseEntity.ok(getStudentDashboardUseCase.getDashboard(id));
    }

    /** Authenticated endpoint — any logged-in user (e.g. a PARENT) can look up a student by their 6-digit link code. */
    @GetMapping("/link-code/{code}")
    public ResponseEntity<StudentProfileResponse> getByLinkCode(@PathVariable String code) {
        return ResponseEntity.ok(studentProfileService.getByLinkCode(code));
    }

    /** Student regenerates their own link code. */
    @PostMapping("/me/link-code/regenerate")
    public ResponseEntity<StudentProfileResponse> regenerateLinkCode(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(studentProfileService.regenerateLinkCode(userId));
    }

    /** Any authenticated user can look up a student by email for linking purposes. */
    @GetMapping("/lookup")
    public ResponseEntity<StudentLookupResponse> lookupByEmail(@RequestParam String email) {
        return ResponseEntity.ok(studentProfileService.lookupByEmail(email));
    }

    /** Parent generates a 6-digit OTP on the student's profile (5-minute expiry). */
    @PostMapping("/link-otp/generate")
    public ResponseEntity<GenerateLinkOtpResponse> generateLinkOtp(
            @RequestHeader("X-User-Id") UUID parentUserId,
            @Valid @RequestBody GenerateLinkOtpRequest request) {
        return ResponseEntity.ok(studentProfileService.generateLinkOtp(request, parentUserId));
    }

    /** Student polls this endpoint to see if a parent has generated an OTP for them. */
    @GetMapping("/me/pending-link")
    public ResponseEntity<PendingLinkResponse> getPendingLink(
            @RequestHeader("X-User-Id") UUID userId) {
        PendingLinkResponse response = studentProfileService.getPendingLink(userId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }

    /** Parent verifies the OTP shared by their child. Returns studentId/name for the parent-svc link call. */
    @PostMapping("/link-otp/verify")
    public ResponseEntity<VerifyLinkOtpResponse> verifyLinkOtp(
            @Valid @RequestBody VerifyLinkOtpRequest request) {
        return ResponseEntity.ok(studentProfileService.verifyLinkOtp(request));
    }
}
