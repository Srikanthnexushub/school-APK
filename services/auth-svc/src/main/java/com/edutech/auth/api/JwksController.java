package com.edutech.auth.api;

import com.edutech.auth.domain.port.in.GetJwksUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class JwksController {

    private final GetJwksUseCase getJwksUseCase;

    public JwksController(GetJwksUseCase getJwksUseCase) {
        this.getJwksUseCase = getJwksUseCase;
    }

    @GetMapping(value = "/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "JSON Web Key Set — public keys for JWT verification")
    public Map<String, Object> getJwks() {
        return getJwksUseCase.getJwks();
    }
}
