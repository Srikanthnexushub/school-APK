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
            case ANTHROPIC -> isPlaceholderKey() ? executeLocalEcho(request) : executeAnthropic(request);
            case OPENAI -> Mono.error(new AiProviderException("OpenAI completions not supported"));
            case OLLAMA -> Mono.error(new AiProviderException("Ollama routing not implemented in this adapter"));
        };
    }

    /** Returns true when no real Anthropic key is configured (local dev placeholder). */
    private boolean isPlaceholderKey() {
        String key = props.apiKey();
        return key == null || key.isBlank()
                || key.startsWith("sk-ant-dev")
                || key.equals("sk-ant-dev-placeholder");
    }

    /**
     * Rule-based fallback used in local dev when no real LLM key is present.
     * Inspects systemPrompt keywords to return a contextually appropriate response
     * so the UI remains fully usable without an external API subscription.
     */
    private Mono<CompletionResponse> executeLocalEcho(CompletionRequest request) {
        String sys  = request.systemPrompt() != null ? request.systemPrompt().toLowerCase() : "";
        String user = request.userMessage()   != null ? request.userMessage().toLowerCase()  : "";

        String reply;

        if (sys.contains("parent copilot") || sys.contains("parent")) {
            if (user.contains("doing") || user.contains("progress") || user.contains("performance")) {
                reply = "Your child is actively enrolled and has been completing assessments. "
                      + "Their latest Exam Readiness Score (ERS) reflects recent test activity. "
                      + "I recommend checking the Performance tab for a detailed breakdown of subject-wise scores and weak areas. "
                      + "Regular practice in flagged weak areas will help improve the readiness score significantly.";
            } else if (user.contains("weak") || user.contains("topic")) {
                reply = "Based on recent assessments, your child's weak areas are tracked in the Performance module. "
                      + "Focus areas are identified automatically after each exam. "
                      + "Encourage daily practice on flagged topics — even 20 minutes per day can make a measurable difference over two weeks.";
            } else if (user.contains("fee") || user.contains("payment") || user.contains("outstanding")) {
                reply = "There is an outstanding fee for the current term. "
                      + "Please visit the fee section on this dashboard for the exact amount and due date. "
                      + "You can pay directly at the center or request an online payment link from the admin.";
            } else if (user.contains("exam") || user.contains("test") || user.contains("schedule")) {
                reply = "Your child has upcoming assessments scheduled. "
                      + "Please check the Exam Tracker for specific dates and subjects. "
                      + "Ensure your child revises the relevant chapters at least 3 days before each exam for best results.";
            } else if (user.contains("mentor") || user.contains("session") || user.contains("book")) {
                reply = "You can book a 1-on-1 mentor session directly from the Mentors section. "
                      + "Sessions are typically 45–60 minutes and can be scheduled for evenings or weekends. "
                      + "I recommend booking at least 3 days in advance to secure your preferred time slot.";
            } else {
                reply = "I'm here to help you stay on top of your child's academic journey. "
                      + "You can ask me about performance trends, exam schedules, fee status, weak areas, or how to book mentor sessions. "
                      + "What would you like to know?";
            }
        } else if (sys.contains("tutor") || sys.contains("doubt") || sys.contains("academic")) {
            reply = "Great question! This topic is fundamental to your exam preparation. "
                  + "I'd recommend reviewing the core concept from your textbook first, then working through 2–3 solved examples. "
                  + "If the doubt persists, consider flagging it for your assigned mentor during the next session. "
                  + "Practice problems on this topic are also available in your assessment library.";
        } else {
            reply = "I've received your message. "
                  + "For the best experience, please configure a valid API key in the platform settings. "
                  + "In the meantime, I can help you navigate the platform features — just ask!";
        }

        CompletionResponse response = new CompletionResponse(
                UUID.randomUUID().toString(),
                reply,
                LlmProvider.ANTHROPIC,
                "local-echo",
                0,
                reply.split("\\s+").length,
                1L
        );
        return Mono.just(response);
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
