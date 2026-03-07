package com.edutech.psych.api;

import com.edutech.psych.application.dto.AuthPrincipal;
import com.edutech.psych.application.dto.CareerMappingResponse;
import com.edutech.psych.application.service.CareerMappingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/psych/profiles/{profileId}/career-mappings")
@Tag(name = "Career Mappings", description = "AI-driven career mapping endpoints")
@SecurityRequirement(name = "BearerAuth")
public class CareerMappingController {

    private final CareerMappingService careerMappingService;

    public CareerMappingController(CareerMappingService careerMappingService) {
        this.careerMappingService = careerMappingService;
    }

    @PostMapping
    public ResponseEntity<CareerMappingResponse> requestCareerMapping(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        CareerMappingResponse response = careerMappingService.requestCareerMapping(profileId, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CareerMappingResponse>> getCareerMappings(
            @PathVariable UUID profileId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<CareerMappingResponse> responses = careerMappingService.getCareerMappings(profileId, principal);
        return ResponseEntity.ok(responses);
    }
}
