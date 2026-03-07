package com.edutech.aigateway.application.service;

import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.application.exception.AiGatewayException;
import com.edutech.aigateway.application.exception.AiProviderException;
import com.edutech.aigateway.application.exception.RateLimitExceededException;
import com.edutech.aigateway.domain.event.AiRequestFailedEvent;
import com.edutech.aigateway.domain.event.AiRequestRoutedEvent;
import com.edutech.aigateway.domain.model.CompletionRequest;
import com.edutech.aigateway.domain.model.CompletionResponse;
import com.edutech.aigateway.domain.model.LlmProvider;
import com.edutech.aigateway.domain.model.ModelType;
import com.edutech.aigateway.domain.port.in.RouteCompletionUseCase;
import com.edutech.aigateway.domain.port.out.AiGatewayEventPublisher;
import com.edutech.aigateway.domain.port.out.LlmClient;
import com.edutech.aigateway.domain.port.out.RateLimitPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class LlmRoutingService implements RouteCompletionUseCase {

    private static final Logger log = LoggerFactory.getLogger(LlmRoutingService.class);

    private final LlmClient llmClient;
    private final RateLimitPort rateLimitPort;
    private final AiGatewayEventPublisher eventPublisher;

    public LlmRoutingService(LlmClient llmClient,
                             RateLimitPort rateLimitPort,
                             AiGatewayEventPublisher eventPublisher) {
        this.llmClient = llmClient;
        this.rateLimitPort = rateLimitPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<CompletionResponse> routeCompletion(CompletionRequest request, AuthPrincipal principal) {
        return rateLimitPort.checkAndIncrement(request.requesterId(), ModelType.COMPLETION)
                .flatMap(allowed -> {
                    if (!allowed) {
                        return Mono.error(new RateLimitExceededException(request.requesterId(), ModelType.COMPLETION));
                    }
                    long start = System.currentTimeMillis();
                    return llmClient.complete(request, LlmProvider.ANTHROPIC)
                            .doOnSuccess(response -> {
                                if (response == null) return;
                                long latency = System.currentTimeMillis() - start;
                                eventPublisher.publish(new AiRequestRoutedEvent(
                                        response.requestId(),
                                        request.requesterId(),
                                        ModelType.COMPLETION,
                                        LlmProvider.ANTHROPIC,
                                        latency));
                                log.info("Completion routed: requesterId={} provider={} tokens={}",
                                        request.requesterId(),
                                        LlmProvider.ANTHROPIC,
                                        response.outputTokens());
                            })
                            .doOnError(e -> {
                                eventPublisher.publish(new AiRequestFailedEvent(
                                        UUID.randomUUID().toString(),
                                        request.requesterId(),
                                        ModelType.COMPLETION,
                                        e.getMessage()));
                                log.error("Completion routing failed: {}", e.getMessage());
                            })
                            .onErrorMap(
                                    e -> !(e instanceof AiGatewayException),
                                    e -> new AiProviderException(e.getMessage()));
                });
    }
}
