package com.edutech.aigateway.application.service;

import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.application.exception.AiGatewayException;
import com.edutech.aigateway.application.exception.AiProviderException;
import com.edutech.aigateway.application.exception.RateLimitExceededException;
import com.edutech.aigateway.domain.event.AiRequestFailedEvent;
import com.edutech.aigateway.domain.event.AiRequestRoutedEvent;
import com.edutech.aigateway.domain.model.EmbeddingRequest;
import com.edutech.aigateway.domain.model.EmbeddingResponse;
import com.edutech.aigateway.domain.model.LlmProvider;
import com.edutech.aigateway.domain.model.ModelType;
import com.edutech.aigateway.domain.port.in.RouteEmbeddingUseCase;
import com.edutech.aigateway.domain.port.out.AiGatewayEventPublisher;
import com.edutech.aigateway.domain.port.out.EmbeddingClient;
import com.edutech.aigateway.domain.port.out.RateLimitPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class EmbeddingService implements RouteEmbeddingUseCase {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingClient embeddingClient;
    private final RateLimitPort rateLimitPort;
    private final AiGatewayEventPublisher eventPublisher;

    public EmbeddingService(EmbeddingClient embeddingClient,
                            RateLimitPort rateLimitPort,
                            AiGatewayEventPublisher eventPublisher) {
        this.embeddingClient = embeddingClient;
        this.rateLimitPort = rateLimitPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<EmbeddingResponse> routeEmbedding(EmbeddingRequest request, AuthPrincipal principal) {
        return rateLimitPort.checkAndIncrement(request.requesterId(), ModelType.EMBEDDING)
                .flatMap(allowed -> {
                    if (!allowed) {
                        return Mono.error(new RateLimitExceededException(request.requesterId(), ModelType.EMBEDDING));
                    }
                    long start = System.currentTimeMillis();
                    return embeddingClient.embed(request)
                            .doOnSuccess(response -> {
                                long latency = System.currentTimeMillis() - start;
                                eventPublisher.publish(new AiRequestRoutedEvent(
                                        response.requestId(),
                                        request.requesterId(),
                                        ModelType.EMBEDDING,
                                        LlmProvider.OPENAI,
                                        latency));
                                log.info("Embedding routed: requesterId={} model={} dimensions={}",
                                        request.requesterId(),
                                        response.modelUsed(),
                                        response.embedding().size());
                            })
                            .doOnError(e -> {
                                eventPublisher.publish(new AiRequestFailedEvent(
                                        UUID.randomUUID().toString(),
                                        request.requesterId(),
                                        ModelType.EMBEDDING,
                                        e.getMessage()));
                                log.error("Embedding routing failed: {}", e.getMessage());
                            })
                            .onErrorMap(
                                    e -> !(e instanceof AiGatewayException),
                                    e -> new AiProviderException(e.getMessage()));
                });
    }
}
