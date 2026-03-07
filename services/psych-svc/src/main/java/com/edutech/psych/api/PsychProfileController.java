package com.edutech.psych.api;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.application.dto.CreatePsychProfileRequest;
import com.edutech.psych.application.dto.PsychProfileResponse;
import com.edutech.psych.application.exception.PsychProfileNotFoundException;
import com.edutech.psych.application.service.PsychProfileService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/psych/profiles")
@Tag(name = "Psych Profiles", description = "Psychometric profile management endpoints")
@SecurityRequirement(name = "BearerAuth")
public class PsychProfileController {

    private final PsychProfileService psychProfileService;

    public PsychProfileController(PsychProfileService psychProfileService) {
        this.psychProfileService = psychProfileService;
    }

    @PostMapping
    public ResponseEntity<PsychProfileResponse> createProfile(
            @Valid @RequestBody CreatePsychProfileRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        PsychProfileResponse response = psychProfileService.createProfile(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<PsychProfileResponse> getProfile(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        PsychProfileResponse response = psychProfileService.getProfile(profileId, principal)
                .orElseThrow(() -> new PsychProfileNotFoundException(profileId));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PsychProfileResponse>> listByCenterId(
            @RequestParam UUID centerId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<PsychProfileResponse> responses = psychProfileService.listByCenterId(centerId, principal);
        return ResponseEntity.ok(responses);
    }
}
