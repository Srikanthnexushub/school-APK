package com.edutech.aigateway.application.service;

import com.edutech.aigateway.application.dto.AuthPrincipal;
import com.edutech.aigateway.application.exception.AiGatewayException;
import com.edutech.aigateway.application.exception.AiProviderException;
import com.edutech.aigateway.application.exception.RateLimitExceededException;
import com.edutech.aigateway.domain.event.AiRequestFailedEvent;
import com.edutech.aigateway.domain.event.AiRequestRoutedEvent;
import com.edutech.aigateway.domain.model.CareerPredictionRequest;
import com.edutech.aigateway.domain.model.CareerPredictionResponse;
import com.edutech.aigateway.domain.model.LlmProvider;
import com.edutech.aigateway.domain.model.ModelType;
import com.edutech.aigateway.domain.port.in.RouteCareerPredictionUseCase;
import com.edutech.aigateway.domain.port.out.AiGatewayEventPublisher;
import com.edutech.aigateway.domain.port.out.PsychAiSidecarClient;
import com.edutech.aigateway.domain.port.out.RateLimitPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class CareerPredictionRoutingService implements RouteCareerPredictionUseCase {

    private static final Logger log = LoggerFactory.getLogger(CareerPredictionRoutingService.class);

    private final PsychAiSidecarClient psychAiSidecarClient;
    private final RateLimitPort rateLimitPort;
    private final AiGatewayEventPublisher eventPublisher;

    public CareerPredictionRoutingService(PsychAiSidecarClient psychAiSidecarClient,
                                          RateLimitPort rateLimitPort,
                                          AiGatewayEventPublisher eventPublisher) {
        this.psychAiSidecarClient = psychAiSidecarClient;
        this.rateLimitPort = rateLimitPort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Mono<CareerPredictionResponse> routeCareerPrediction(CareerPredictionRequest request,
                                                                 AuthPrincipal principal) {
        return rateLimitPort.checkAndIncrement(request.requesterId(), ModelType.CAREER_PREDICTION)
                .flatMap(allowed -> {
                    if (!allowed) {
                        return Mono.error(new RateLimitExceededException(
                                request.requesterId(), ModelType.CAREER_PREDICTION));
                    }
                    long start = System.currentTimeMillis();
                    return psychAiSidecarClient.predictCareers(request)
                            .doOnSuccess(response -> {
                                long latency = System.currentTimeMillis() - start;
                                eventPublisher.publish(new AiRequestRoutedEvent(
                                        response.requestId(),
                                        request.requesterId(),
                                        ModelType.CAREER_PREDICTION,
                                        LlmProvider.OLLAMA,
                                        latency));
                                log.info("Career prediction routed: requesterId={} profileId={} topCareers={}",
                                        request.requesterId(),
                                        request.profileId(),
                                        response.topCareers());
                            })
                            .doOnError(e -> {
                                eventPublisher.publish(new AiRequestFailedEvent(
                                        UUID.randomUUID().toString(),
                                        request.requesterId(),
                                        ModelType.CAREER_PREDICTION,
                                        e.getMessage()));
                                log.error("Career prediction routing failed: {}", e.getMessage());
                            })
                            .onErrorMap(
                                    e -> !(e instanceof AiGatewayException),
                                    e -> new AiProviderException(e.getMessage()));
                });
    }
}
