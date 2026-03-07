package com.edutech.aigateway.api;

import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.domain.model.CompletionRequest;
import com.edutech.aigateway.domain.model.CompletionResponse;
import com.edutech.aigateway.domain.port.in.RouteCompletionUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/ai/completions")
public class CompletionController {

    private final RouteCompletionUseCase routeCompletionUseCase;

    public CompletionController(RouteCompletionUseCase routeCompletionUseCase) {
        this.routeCompletionUseCase = routeCompletionUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<CompletionResponse> complete(
            @Valid @RequestBody CompletionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return routeCompletionUseCase.routeCompletion(request, principal);
    }
}
