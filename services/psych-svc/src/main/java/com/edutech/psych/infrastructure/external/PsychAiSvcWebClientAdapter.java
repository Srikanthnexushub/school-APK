package com.edutech.psych.infrastructure.external;

import com.edutech.psych.application.config.PsychAiSvcProperties;
import com.edutech.psych.domain.port.out.CareerPredictionResponse;
import com.edutech.psych.domain.port.out.PsychAiSvcClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Component
public class PsychAiSvcWebClientAdapter implements PsychAiSvcClient {

    private static final Logger log = LoggerFactory.getLogger(PsychAiSvcWebClientAdapter.class);
    private static final String PREDICT_CAREERS_PATH = "/api/v1/predict-careers";

    private final WebClient webClient;

    public PsychAiSvcWebClientAdapter(WebClient.Builder webClientBuilder, PsychAiSvcProperties props) {
        this.webClient = webClientBuilder
                .baseUrl(props.baseUrl())
                .build();
    }

    @Override
    @CircuitBreaker(name = "psych-ai-svc")
    public CareerPredictionResponse predictCareers(UUID profileId,
                                                   double openness,
                                                   double conscientiousness,
                                                   double extraversion,
                                                   double agreeableness,
                                                   double neuroticism,
                                                   String riasecCode) {
        PredictRequest requestBody = new PredictRequest(
                profileId.toString(),
                openness,
                conscientiousness,
                extraversion,
                agreeableness,
                neuroticism,
                riasecCode
        );

        try {
            PredictResponse response = webClient.post()
                    .uri(PREDICT_CAREERS_PATH)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(PredictResponse.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Career prediction failed: empty response from AI service");
            }

            return new CareerPredictionResponse(
                    response.topCareers(),
                    response.reasoning(),
                    response.modelVersion()
            );
        } catch (RuntimeException ex) {
            log.error("Career prediction request failed for profileId {}: {}", profileId, ex.getMessage(), ex);
            throw new RuntimeException("Career prediction failed", ex);
        }
    }

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
