package com.edutech.auth.api;

import com.edutech.auth.application.dto.CaptchaChallengeResponse;
import com.edutech.auth.application.service.LocalCaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/captcha")
@Tag(name = "CAPTCHA", description = "Server-generated image CAPTCHA challenge")
public class CaptchaController {

    private final LocalCaptchaService captchaService;

    public CaptchaController(LocalCaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("/challenge")
    @Operation(summary = "Generate a new image CAPTCHA challenge. Returns id and base64 PNG data URI.")
    public ResponseEntity<CaptchaChallengeResponse> challenge() {
        return ResponseEntity.ok(captchaService.generate());
    }
}
