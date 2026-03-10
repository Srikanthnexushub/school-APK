package com.edutech.aigateway.infrastructure.external;

import com.edutech.aigateway.application.exception.AiProviderException;
import com.edutech.aigateway.domain.model.CompletionRequest;
import com.edutech.aigateway.domain.model.CompletionResponse;
import com.edutech.aigateway.domain.model.LlmProvider;
import com.edutech.aigateway.domain.port.out.LlmClient;
import com.edutech.aigateway.infrastructure.config.OpenRouterProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("openRouterLlmClient")
public class OpenRouterWebClientAdapter implements LlmClient {

    private final WebClient webClient;
    private final OpenRouterProperties props;

    public OpenRouterWebClientAdapter(WebClient.Builder builder, OpenRouterProperties props) {
        this.props = props;
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.connectTimeoutMs())
                .doOnConnected(conn -> conn.addHandlerLast(
                        new ReadTimeoutHandler(props.readTimeoutMs(), TimeUnit.MILLISECONDS)));
        this.webClient = builder
                .baseUrl(props.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public Mono<CompletionResponse> complete(CompletionRequest request, LlmProvider provider) {
        return switch (provider) {
            case OPENROUTER -> isPlaceholderKey() ? executeLocalEcho(request) : executeOpenRouter(request);
            case ANTHROPIC -> Mono.error(new AiProviderException("Anthropic completions not supported in OpenRouter adapter"));
            case OPENAI -> Mono.error(new AiProviderException("OpenAI completions not supported in OpenRouter adapter"));
            case OLLAMA -> Mono.error(new AiProviderException("Ollama routing not implemented in this adapter"));
        };
    }

    /** Returns true when no real OpenRouter key is configured (local dev placeholder). */
    public boolean isPlaceholderKey() {
        String key = props.apiKey();
        return key == null || key.isBlank()
                || key.startsWith("or-dev")
                || key.equals("or-dev-placeholder");
    }

    /**
     * Rule-based fallback used in local dev when no real LLM key is present.
     * Inspects systemPrompt keywords to return a contextually appropriate response
     * so the UI remains fully usable without an external API subscription.
     */
    public Mono<CompletionResponse> executeLocalEcho(CompletionRequest request) {
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
            } else if (user.contains("psychometric") || user.contains("personality") || user.contains("riasec")
                    || user.contains("career") || user.contains("big five") || user.contains("trait")) {
                reply = buildPsychReplyFromContext(request.systemPrompt());
            } else {
                reply = "I'm here to help you stay on top of your child's academic journey. "
                      + "You can ask me about performance trends, exam schedules, fee status, weak areas, psychometric results, career recommendations, or how to book mentor sessions. "
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
                LlmProvider.OPENROUTER,
                "local-echo",
                0,
                reply.split("\\s+").length,
                1L
        );
        return Mono.just(response);
    }

    private String buildPsychReplyFromContext(String systemPrompt) {
        if (systemPrompt == null || !systemPrompt.contains("Student Psychometric Profile")) {
            return "Your child's psychometric profile hasn't been completed yet, or no student is linked to this conversation. "
                 + "Once the assessment is taken, you'll be able to see their Big Five personality scores, RIASEC career code, and learning style recommendations here.";
        }
        String o = extractScore(systemPrompt, "Openness");
        String c = extractScore(systemPrompt, "Conscientiousness");
        String e = extractScore(systemPrompt, "Extraversion");
        String a = extractScore(systemPrompt, "Agreeableness");
        String n = extractScore(systemPrompt, "Neuroticism");
        String riasec = extractField(systemPrompt, "RIASEC Career Code: ([A-Z-]+)");
        String learning = extractField(systemPrompt, "Dominant Learning Style: ([^\n]+)");

        return "Your child's psychometric assessment results:\n\n"
             + "Big Five Personality Profile:\n"
             + "• Openness: " + o + "/100 — high intellectual curiosity and creativity\n"
             + "• Conscientiousness: " + c + "/100 — excellent organisation and study discipline\n"
             + "• Extraversion: " + e + "/100 — balanced social energy\n"
             + "• Agreeableness: " + a + "/100 — cooperative and empathetic in groups\n"
             + "• Neuroticism: " + n + "/100 — resilient under pressure (lower is better)\n\n"
             + "RIASEC Career Code: " + riasec + "\n"
             + "Dominant Learning Style: " + learning + "\n\n"
             + "Recommended career paths: Research Scientist, Data Analyst, Psychologist, Educator, Software Architect.\n"
             + "Encourage your child to use visual study techniques: diagrams, colour-coded notes, and video-based learning.";
    }

    private String extractScore(String text, String traitName) {
        Matcher m = Pattern.compile(traitName + ":\\s*(\\d+)/100", Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() ? m.group(1) : "N/A";
    }

    private String extractField(String text, String regex) {
        Matcher m = Pattern.compile(regex).matcher(text);
        return m.find() ? m.group(1).trim() : "N/A";
    }

    private Mono<CompletionResponse> executeOpenRouter(CompletionRequest request) {
        List<OpenRouterMessage> messages = new java.util.ArrayList<>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(new OpenRouterMessage("system", request.systemPrompt()));
        }
        messages.add(new OpenRouterMessage("user", request.userMessage()));

        OpenRouterRequest body = new OpenRouterRequest(
                props.model(),
                messages,
                request.maxTokens(),
                request.temperature()
        );
        long startMs = System.currentTimeMillis();

        return webClient.post()
                .uri("/api/v1/chat/completions")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(
                                        new AiProviderException("OpenRouter API error [" +
                                                clientResponse.statusCode().value() + "]: " + errorBody)))
                )
                .bodyToMono(OpenRouterResponse.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(ex -> !(ex instanceof AiProviderException)))
                .map(response -> {
                    long latencyMs = System.currentTimeMillis() - startMs;
                    String content = (response.choices() != null && !response.choices().isEmpty())
                            ? response.choices().get(0).message().content()
                            : "";
                    int inputTokens = (response.usage() != null) ? response.usage().promptTokens() : 0;
                    int outputTokens = (response.usage() != null) ? response.usage().completionTokens() : 0;

                    return new CompletionResponse(
                            UUID.randomUUID().toString(),
                            content,
                            LlmProvider.OPENROUTER,
                            props.model(),
                            inputTokens,
                            outputTokens,
                            latencyMs
                    );
                });
    }

    // ── Private request/response records ───────────────────────────────────────

    private record OpenRouterRequest(
            String model,
            List<OpenRouterMessage> messages,
            @JsonProperty("max_tokens") int maxTokens,
            double temperature
    ) {}

    private record OpenRouterMessage(
            String role,
            String content
    ) {}

    private record OpenRouterResponse(
            String id,
            List<OpenRouterChoice> choices,
            OpenRouterUsage usage
    ) {}

    private record OpenRouterChoice(
            OpenRouterMessage message
    ) {}

    private record OpenRouterUsage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("completion_tokens") int completionTokens
    ) {}
}
