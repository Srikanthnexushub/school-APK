package com.edutech.mentorsvc.api;

import com.edutech.mentorsvc.application.dto.MentorProfileResponse;
import com.edutech.mentorsvc.application.dto.RegisterMentorRequest;
import com.edutech.mentorsvc.application.dto.UpdateMentorAvailabilityRequest;
import com.edutech.mentorsvc.domain.port.in.GetMentorProfileUseCase;
import com.edutech.mentorsvc.domain.port.in.RegisterMentorUseCase;
import com.edutech.mentorsvc.domain.port.in.UpdateMentorAvailabilityUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mentors")
@Tag(name = "Mentor Profiles", description = "Manage mentor profiles")
public class MentorProfileController {

    private final RegisterMentorUseCase registerMentorUseCase;
    private final GetMentorProfileUseCase getMentorProfileUseCase;
    private final UpdateMentorAvailabilityUseCase updateMentorAvailabilityUseCase;

    public MentorProfileController(RegisterMentorUseCase registerMentorUseCase,
                                   GetMentorProfileUseCase getMentorProfileUseCase,
                                   UpdateMentorAvailabilityUseCase updateMentorAvailabilityUseCase) {
        this.registerMentorUseCase = registerMentorUseCase;
        this.getMentorProfileUseCase = getMentorProfileUseCase;
        this.updateMentorAvailabilityUseCase = updateMentorAvailabilityUseCase;
    }

    @PostMapping
    @Operation(summary = "Register a new mentor")
    public ResponseEntity<MentorProfileResponse> registerMentor(
            @Valid @RequestBody RegisterMentorRequest request) {
        MentorProfileResponse response = registerMentorUseCase.registerMentor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{mentorId}")
    @Operation(summary = "Get mentor profile by ID")
    public ResponseEntity<MentorProfileResponse> getMentor(@PathVariable UUID mentorId) {
        return ResponseEntity.ok(getMentorProfileUseCase.getMentorById(mentorId));
    }

    @GetMapping
    @Operation(summary = "List all available mentors")
    public ResponseEntity<List<MentorProfileResponse>> getAllAvailableMentors() {
        return ResponseEntity.ok(getMentorProfileUseCase.getAllAvailableMentors());
    }

    @PatchMapping("/{mentorId}/availability")
    @Operation(summary = "Update mentor availability")
    public ResponseEntity<Void> updateAvailability(
            @PathVariable UUID mentorId,
            @Valid @RequestBody UpdateMentorAvailabilityRequest request) {
        updateMentorAvailabilityUseCase.updateAvailability(mentorId, request.available());
        return ResponseEntity.noContent().build();
    }
}
