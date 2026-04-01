package com.edutech.aigateway.domain.port.out;

import com.edutech.aigateway.domain.model.CompletionRequest;
import com.edutech.aigateway.domain.model.CompletionResponse;
import com.edutech.aigateway.domain.model.LlmProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LlmClient {
    Mono<CompletionResponse> complete(CompletionRequest request, LlmProvider provider);

    /**
     * Streams token deltas as raw OpenRouter delta JSON (no SSE prefix).
     * Each emitted string is the raw JSON payload that will be wrapped by the
     * Spring SSE encoder as {@code data: {payload}\n\n}.
     * Terminated by the sentinel string {@code [DONE]}.
     * Default falls back to blocking completion, emits content as a single chunk.
     */
    default Flux<String> streamComplete(CompletionRequest request, LlmProvider provider) {
        return complete(request, provider)
                .flatMapMany(resp -> {
                    String content = resp.content() != null ? resp.content() : "";
                    String delta = "{\"choices\":[{\"delta\":{\"content\":"
                            + toJsonString(content) + "}}]}";
                    return Flux.just(delta, "[DONE]");
                });
    }

    private static String toJsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }
}
