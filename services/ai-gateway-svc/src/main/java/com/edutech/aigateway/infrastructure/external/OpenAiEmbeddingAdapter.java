package com.edutech.aigateway.infrastructure.external;

import com.edutech.aigateway.application.config.OpenAiProperties;
import com.edutech.aigateway.application.exception.AiProviderException;
import com.edutech.aigateway.domain.model.EmbeddingRequest;
import com.edutech.aigateway.domain.model.EmbeddingResponse;
import com.edutech.aigateway.domain.port.out.EmbeddingClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
public class OpenAiEmbeddingAdapter implements EmbeddingClient {

    private final WebClient webClient;
    private final OpenAiProperties props;

    public OpenAiEmbeddingAdapter(WebClient.Builder builder, OpenAiProperties props) {
        this.props = props;
        this.webClient = builder
                .baseUrl("https://api.openai.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Mono<EmbeddingResponse> embed(EmbeddingRequest request) {
        OpenAiEmbedRequest body = new OpenAiEmbedRequest(props.embeddingModel(), request.text());
        long startMs = System.currentTimeMillis();

        return webClient.post()
                .uri("/v1/embeddings")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(
                                        new AiProviderException("OpenAI Embeddings API error [" +
                                                clientResponse.statusCode().value() + "]: " + errorBody)))
                )
                .bodyToMono(OpenAiEmbedResponse.class)
                .map(response -> {
                    long latencyMs = System.currentTimeMillis() - startMs;

                    if (response.data() == null || response.data().isEmpty()) {
                        throw new AiProviderException("OpenAI returned empty embedding data");
                    }

                    List<Double> embedding = response.data().get(0).embedding();
                    return new EmbeddingResponse(
                            UUID.randomUUID().toString(),
                            embedding,
                            props.embeddingModel(),
                            latencyMs
                    );
                });
    }

    // ── Private request/response records ───────────────────────────────────────

    private record OpenAiEmbedRequest(
            String model,
            String input
    ) {}

    private record OpenAiEmbedResponse(
            List<OpenAiEmbedData> data
    ) {}

    private record OpenAiEmbedData(
            List<Double> embedding
    ) {}
}
