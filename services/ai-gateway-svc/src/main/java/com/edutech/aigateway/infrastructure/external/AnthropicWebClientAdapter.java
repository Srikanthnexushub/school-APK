package com.edutech.aigateway.infrastructure.external;

import com.edutech.aigateway.application.config.AnthropicProperties;
import com.edutech.aigateway.application.exception.AiProviderException;
import com.edutech.aigateway.domain.model.CompletionRequest;
import com.edutech.aigateway.domain.model.CompletionResponse;
import com.edutech.aigateway.domain.model.LlmProvider;
import com.edutech.aigateway.domain.port.out.LlmClient;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class AnthropicWebClientAdapter implements LlmClient {

    private final WebClient webClient;
    private final AnthropicProperties props;

    public AnthropicWebClientAdapter(WebClient.Builder builder, AnthropicProperties props) {
        this.props = props;
        this.webClient = builder
                .baseUrl(props.baseUrl())
                .defaultHeader("x-api-key", props.apiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Mono<CompletionResponse> complete(CompletionRequest request, LlmProvider provider) {
        return switch (provider) {
            case ANTHROPIC -> executeAnthropic(request);
            case OPENAI -> Mono.error(new AiProviderException("OpenAI completions not supported"));
            case OLLAMA -> Mono.error(new AiProviderException("Ollama routing not implemented in this adapter"));
        };
    }

    private Mono<CompletionResponse> executeAnthropic(CompletionRequest request) {
        List<AnthropicMessage> messages = new ArrayList<>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(new AnthropicMessage("user",
                    "[System]: " + request.systemPrompt() + "\n\n" + request.userMessage()));
        } else {
            messages.add(new AnthropicMessage("user", request.userMessage()));
        }

        AnthropicRequest body = new AnthropicRequest(props.model(), request.maxTokens(), messages);
        long startMs = System.currentTimeMillis();

        return webClient.post()
                .uri("/v1/messages")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(
                                        new AiProviderException("Anthropic API error [" +
                                                clientResponse.statusCode().value() + "]: " + errorBody)))
                )
                .bodyToMono(AnthropicResponse.class)
                .map(response -> {
                    long latencyMs = System.currentTimeMillis() - startMs;
                    String content = (response.content() != null && !response.content().isEmpty())
                            ? response.content().get(0).text()
                            : "";
                    int inputTokens = (response.usage() != null) ? response.usage().inputTokens() : 0;
                    int outputTokens = (response.usage() != null) ? response.usage().outputTokens() : 0;

                    return new CompletionResponse(
                            UUID.randomUUID().toString(),
                            content,
                            LlmProvider.ANTHROPIC,
                            props.model(),
                            inputTokens,
                            outputTokens,
                            latencyMs
                    );
                });
    }

    // ── Private request/response records ───────────────────────────────────────

    private record AnthropicRequest(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            List<AnthropicMessage> messages
    ) {}

    private record AnthropicMessage(
            String role,
            String content
    ) {}

    private record AnthropicResponse(
            String id,
            List<AnthropicContent> content,
            AnthropicUsage usage
    ) {}

    private record AnthropicContent(
            String text
    ) {}

    private record AnthropicUsage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens
    ) {}
}
