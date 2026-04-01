# NexusChat — AI Gateway Streaming Changes

## Files Modified in ai-gateway-svc

### 1. LlmClient.java — add streamCompletion method

```java
// ADD to existing interface:
package com.edutech.aigateway.domain.port.out;

import reactor.core.publisher.Flux;

public interface LlmClient {
    // EXISTING:
    CompletionResponse complete(CompletionRequest request);

    // NEW:
    Flux<String> streamCompletion(StreamCompletionRequest request);
}
```

### 2. StreamCompletionRequest.java — NEW domain model

```java
package com.edutech.aigateway.domain.model;

import java.util.List;
import java.util.Map;

public record StreamCompletionRequest(
    String requesterId,
    String systemPrompt,
    List<Map<String, String>> history,   // conversation history: [{role, content}]
    String userMessage,
    int maxTokens,
    double temperature
) {}
```

### 3. OpenRouterWebClientAdapter.java — add streamCompletion

```java
// ADD this method to existing OpenRouterWebClientAdapter class:

@Override
public Flux<String> streamCompletion(StreamCompletionRequest request) {
    // Check for placeholder key — return local echo stream
    if (apiKey == null || apiKey.startsWith("sk-or-placeholder") ||
        apiKey.equals("YOUR_OPENROUTER_KEY_HERE")) {
        return buildLocalEchoStream(request.userMessage());
    }

    List<Map<String, String>> messages = buildMessages(request);

    Map<String, Object> body = Map.of(
        "model", model,
        "messages", messages,
        "max_tokens", request.maxTokens(),
        "temperature", request.temperature(),
        "stream", true   // KEY: enables SSE streaming from OpenRouter
    );

    return webClient.post()
        .uri("/api/v1/chat/completions")
        .header("Authorization", "Bearer " + apiKey)
        .header("HTTP-Referer", "https://nexused.ai")
        .header("X-Title", "NexusEd Platform")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .bodyToFlux(String.class)   // raw SSE lines
        .filter(line -> line.startsWith("data: ") && !line.contains("[DONE]"))
        .mapNotNull(this::extractDeltaContent)
        .onErrorResume(e -> {
            log.error("OpenRouter stream error: {}", e.getMessage());
            return Flux.error(e);
        });
}

private List<Map<String, String>> buildMessages(StreamCompletionRequest request) {
    List<Map<String, String>> msgs = new ArrayList<>();
    if (request.systemPrompt() != null) {
        msgs.add(Map.of("role", "system", "content", request.systemPrompt()));
    }
    if (request.history() != null) {
        msgs.addAll(request.history());
    }
    msgs.add(Map.of("role", "user", "content", request.userMessage()));
    return msgs;
}

private String extractDeltaContent(String line) {
    // Parse: data: {"choices":[{"delta":{"content":"token"},...}],...}
    String json = line.substring(6).trim();
    int idx = json.indexOf("\"content\":\"");
    if (idx == -1) return null;
    int start = idx + 11;
    int end = json.indexOf("\"", start);
    if (end == -1) return null;
    String token = json.substring(start, end);
    return token.isEmpty() ? null : token
        .replace("\\n", "\n")
        .replace("\\t", "\t")
        .replace("\\\"", "\"");
}

private Flux<String> buildLocalEchoStream(String userMessage) {
    // Local echo for dev — splits response into word tokens with 30ms delay
    String echoResponse = "[LOCAL ECHO] You asked: " + userMessage +
        ". This is a simulated streaming response for local development. " +
        "Connect a real API key in .env to get actual AI responses.";
    return Flux.fromArray(echoResponse.split(" "))
        .map(word -> word + " ")
        .delayElements(java.time.Duration.ofMillis(30));
}
```

### 4. AnthropicWebClientAdapter.java — add streamCompletion

```java
// ADD this method to existing AnthropicWebClientAdapter class:

@Override
public Flux<String> streamCompletion(StreamCompletionRequest request) {
    if (apiKey == null || apiKey.startsWith("sk-ant-dev-placeholder")) {
        return buildLocalEchoStream(request.userMessage());
    }

    List<Map<String, Object>> messages = new ArrayList<>();
    if (request.history() != null) {
        for (var h : request.history()) {
            messages.add(Map.of("role", h.get("role"), "content", h.get("content")));
        }
    }
    messages.add(Map.of("role", "user", "content", request.userMessage()));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);
    body.put("max_tokens", request.maxTokens());
    body.put("stream", true);
    if (request.systemPrompt() != null) body.put("system", request.systemPrompt());
    body.put("messages", messages);

    return webClient.post()
        .uri("/v1/messages")
        .header("x-api-key", apiKey)
        .header("anthropic-version", "2023-06-01")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve()
        .bodyToFlux(String.class)
        .filter(line -> line.startsWith("data: "))
        .mapNotNull(this::extractAnthropicDelta)
        .onErrorResume(e -> {
            log.error("Anthropic stream error: {}", e.getMessage());
            return Flux.error(e);
        });
}

private String extractAnthropicDelta(String line) {
    // Anthropic SSE format: data: {"type":"content_block_delta","delta":{"text":"token"}}
    String json = line.substring(6).trim();
    if (!json.contains("content_block_delta")) return null;
    int idx = json.indexOf("\"text\":\"");
    if (idx == -1) return null;
    int start = idx + 8;
    int end = json.indexOf("\"", start);
    return end == -1 ? null : json.substring(start, end);
}

private Flux<String> buildLocalEchoStream(String userMessage) {
    String echo = "[LOCAL ECHO] " + userMessage;
    return Flux.fromArray(echo.split(" "))
        .map(w -> w + " ")
        .delayElements(java.time.Duration.ofMillis(40));
}
```

### 5. LlmRoutingService.java — add streamCompletion routing

```java
// ADD this method to existing LlmRoutingService class:

public Flux<String> streamCompletion(StreamCompletionRequest request) {
    // Check rate limit (reuse existing checkRateLimit)
    checkRateLimit(request.requesterId(), ModelType.COMPLETION);

    LlmProvider provider = resolveProvider();
    log.debug("Streaming via provider={} for requester={}", provider, request.requesterId());

    return switch (provider) {
        case ANTHROPIC   -> anthropicClient.streamCompletion(request);
        case OPENROUTER  -> openRouterClient.streamCompletion(request);
        case OLLAMA      -> {
            // Ollama doesn't support streaming in current setup — fall back to blocking
            CompletionResponse r = ollamaClient.complete(
                new CompletionRequest(request.requesterId(), request.systemPrompt(),
                    request.userMessage(), request.maxTokens(), request.temperature()));
            yield Flux.just(r.content());
        }
        default          -> openRouterClient.streamCompletion(request);
    };
}
```

### 6. CompletionController.java — add /stream endpoint

```java
// ADD to existing CompletionController class:

@PostMapping(value = "/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamCompletion(
        @RequestBody StreamCompletionRequestDto request,
        @RequestHeader("X-Service-Key") String serviceKey) {

    validateServiceKey(serviceKey);

    StreamCompletionRequest domainRequest = new StreamCompletionRequest(
        request.requesterId(),
        request.systemPrompt(),
        request.history(),
        request.userMessage(),
        request.maxTokens() != null ? request.maxTokens() : 1200,
        request.temperature() != null ? request.temperature() : 0.7
    );

    return llmRoutingService.streamCompletion(domainRequest)
        .map(token -> ServerSentEvent.<String>builder()
            .data("data: " + buildDeltaJson(token))
            .build())
        .concatWith(Flux.just(
            ServerSentEvent.<String>builder().data("data: [DONE]").build()
        ))
        .doOnError(e -> log.error("Stream completion error: {}", e.getMessage()));
}

private String buildDeltaJson(String token) {
    String escaped = token.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n").replace("\r", "\\r");
    return "{\"choices\":[{\"delta\":{\"content\":\"" + escaped + "\"}}]}";
}
```

### 7. StreamCompletionRequestDto.java — new API DTO in ai-gateway-svc

```java
package com.edutech.aigateway.api.dto;

import java.util.List;
import java.util.Map;

public record StreamCompletionRequestDto(
    String requesterId,
    String systemPrompt,
    List<Map<String, String>> history,
    String userMessage,
    Integer maxTokens,
    Double temperature
) {}
```

## Summary of changes to ai-gateway-svc

| File | Change Type | What Changed |
|---|---|---|
| `LlmClient.java` | MODIFIED | Added `streamCompletion()` method to interface |
| `StreamCompletionRequest.java` | NEW | Domain model for streaming requests |
| `StreamCompletionRequestDto.java` | NEW | API DTO |
| `OpenRouterWebClientAdapter.java` | MODIFIED | Added `streamCompletion()` + local echo fallback |
| `AnthropicWebClientAdapter.java` | MODIFIED | Added `streamCompletion()` + local echo fallback |
| `LlmRoutingService.java` | MODIFIED | Added `streamCompletion()` routing method |
| `CompletionController.java` | MODIFIED | Added `/completions/stream` endpoint |

**No existing endpoints changed. No existing behaviour altered. Zero breaking changes.**
