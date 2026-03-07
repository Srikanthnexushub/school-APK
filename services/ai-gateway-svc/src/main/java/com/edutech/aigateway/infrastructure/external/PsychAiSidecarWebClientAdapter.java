package com.edutech.aigateway.infrastructure.external;

import com.edutech.aigateway.application.config.PsychAiSidecarProperties;
import com.edutech.aigateway.application.exception.AiProviderException;
import com.edutech.aigateway.domain.model.CareerPredictionRequest;
import com.edutech.aigateway.domain.model.CareerPredictionResponse;
import com.edutech.aigateway.domain.port.out.PsychAiSidecarClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class PsychAiSidecarWebClientAdapter implements PsychAiSidecarClient {

    private final WebClient webClient;

    public PsychAiSidecarWebClientAdapter(WebClient.Builder builder,
                                           PsychAiSidecarProperties props) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) Duration.ofSeconds(props.connectTimeoutSeconds()).toMillis())
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(props.readTimeoutSeconds(), TimeUnit.SECONDS)));

        this.webClient = builder
                .baseUrl(props.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Mono<CareerPredictionResponse> predictCareers(CareerPredictionRequest request) {
        PredictRequest body = new PredictRequest(
                request.profileId(),
                request.openness(),
                request.conscientiousness(),
                request.extraversion(),
                request.agreeableness(),
                request.neuroticism(),
                request.riasecCode()
        );
        long startMs = System.currentTimeMillis();

        return webClient.post()
                .uri("/api/v1/predict-careers")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(
                                        new AiProviderException("PsychAI sidecar error [" +
                                                clientResponse.statusCode().value() + "]: " + errorBody)))
                )
                .bodyToMono(PredictResponse.class)
                .map(response -> {
                    long latencyMs = System.currentTimeMillis() - startMs;
                    return new CareerPredictionResponse(
                            UUID.randomUUID().toString(),
                            response.topCareers(),
                            response.reasoning(),
                            response.modelVersion(),
                            latencyMs
                    );
                });
    }

    // ── Private request/response records ───────────────────────────────────────

    private record PredictRequest(
            String profileId,
            double openness,
            double conscientiousness,
            double extraversion,
            double agreeableness,
            double neuroticism,
            String riasecCode
    ) {}

    private record PredictResponse(
            List<String> topCareers,
            String reasoning,
            String modelVersion
    ) {}
}
