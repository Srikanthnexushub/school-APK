package com.edutech.aigateway.api;

import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.domain.model.EmbeddingRequest;
import com.edutech.aigateway.domain.model.EmbeddingResponse;
import com.edutech.aigateway.domain.port.in.RouteEmbeddingUseCase;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/ai/embeddings")
public class EmbeddingController {

    private final RouteEmbeddingUseCase routeEmbeddingUseCase;

    public EmbeddingController(RouteEmbeddingUseCase routeEmbeddingUseCase) {
        this.routeEmbeddingUseCase = routeEmbeddingUseCase;
    }

    @PostMapping
    public Mono<EmbeddingResponse> embed(
            @Valid @RequestBody EmbeddingRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return routeEmbeddingUseCase.routeEmbedding(request, principal);
    }
}
