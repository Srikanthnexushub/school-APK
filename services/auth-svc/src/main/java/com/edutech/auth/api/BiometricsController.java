// src/main/java/com/edutech/auth/api/BiometricsController.java
package com.edutech.auth.api;

import com.edutech.auth.application.dto.BiometricsRequest;
import com.edutech.auth.application.dto.BiometricsRiskScore;
import com.edutech.auth.application.service.BiometricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/biometrics")
@Tag(name = "Behavioral Biometrics", description = "Keystroke dynamics risk scoring")
public class BiometricsController {

    private final BiometricsService biometricsService;

    public BiometricsController(BiometricsService biometricsService) {
        this.biometricsService = biometricsService;
    }

    @PostMapping
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Analyse keystroke dynamics and return a behavioral risk score")
    public BiometricsRiskScore analyse(@Valid @RequestBody BiometricsRequest request) {
        return biometricsService.calculateRisk(request);
    }
}
