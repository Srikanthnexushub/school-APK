# ai-gateway-svc — COMPLETION RECORD

> **STATUS: FROZEN — DO NOT MODIFY**
> Tests passed: 10/10 | Build: SUCCESS | Date: 2026-03-07

---

## Service Overview

Fully reactive AI gateway service (Spring WebFlux) that routes completion, embedding, and career prediction requests to external LLM providers (Anthropic, OpenAI) and the internal psych-ai-svc sidecar. Enforces per-requester rate limiting via Redis and publishes audit events to Kafka.

---

## Test Results

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS — Total time: 6.910 s
```

| Test Class | Tests | Result |
|---|---|---|
| ArchitectureRulesTest | 5 | PASS |
| LlmRoutingServiceTest | 5 | PASS |

---

## Architecture

Hexagonal (Ports & Adapters) — fully reactive (WebFlux, Mono/Flux). No JPA, no blocking I/O.

```
domain/
  model/        ModelType, LlmProvider, Role, CompletionRequest/Response,
                EmbeddingRequest/Response, CareerPredictionRequest/Response
  event/        AiRequestRoutedEvent, AiRequestFailedEvent
  port/in/      RouteCompletionUseCase, RouteEmbeddingUseCase,
                RouteCareerPredictionUseCase  (all return Mono<T>)
  port/out/     LlmClient, EmbeddingClient, PsychAiSidecarClient,
                RateLimitPort (Mono<Boolean>), AiGatewayEventPublisher (void)

application/
  config/       JwtProperties, AnthropicProperties, OpenAiProperties,
                OllamaProperties, PsychAiSidecarProperties
  dto/          AuthPrincipal (record)
  exception/    AiGatewayException (abstract), RateLimitExceededException,
                AiProviderException, ModelNotFoundException
  service/      LlmRoutingService, EmbeddingService,
                CareerPredictionRoutingService

infrastructure/
  config/       KafkaTopicProperties
  security/     JwtTokenValidator, JwtAuthenticationWebFilter (WebFilter),
                SecurityConfig (@EnableWebFluxSecurity)
  external/     AnthropicWebClientAdapter, OpenAiEmbeddingAdapter,
                PsychAiSidecarWebClientAdapter
  ratelimit/    RedisRateLimitAdapter (ReactiveRedisTemplate)
  messaging/    AiGatewayKafkaAdapter

api/
  CompletionController     POST /api/v1/ai/completions
  EmbeddingController      POST /api/v1/ai/embeddings
  CareerPredictionController POST /api/v1/ai/career-predictions
  GlobalExceptionHandler   RFC 7807 ProblemDetail (Mono<ResponseEntity<ProblemDetail>>)
```

---

## Key Design Decisions

- **Fully reactive**: WebFlux throughout; no blocking calls. `ReactiveRedisTemplate` for rate limiting.
- **Security**: `@EnableWebFluxSecurity`, `ServerHttpSecurity`, `JwtAuthenticationWebFilter` implements `WebFilter` (not `OncePerRequestFilter`).
- **JWT RS256**: JJWT 0.12.x `Jwts.parser().verifyWith(publicKey).requireIssuer(issuer).build().parseSignedClaims(token).getPayload()`.
- **Rate limiting**: Redis INCR + EXPIRE via `ReactiveRedisTemplate`; fails open (`onErrorReturn(true)`) on Redis outage.
- **Reactive service pattern**: `rateLimitPort.checkAndIncrement().flatMap(allowed -> ...).doOnSuccess(r -> { if (r==null) return; ... }).doOnError(...).onErrorMap(e -> !(e instanceof AiGatewayException), AiProviderException::new)`.
- **Event publishing**: Fire-and-forget (`AiGatewayEventPublisher` void); Kafka failures logged, never rolled back.
- **Error mapping**: Non-`AiGatewayException` errors from providers mapped to `AiProviderException` (502); rate limit → 429; model not found → 404.
- **No Lombok**, no hardcoded values, all config via `@ConfigurationProperties` + `${ENV_VAR}` in YAML.

---

## Fixes Applied

1. **Swagger `@Tag` removed** from all 3 controllers — `io.swagger.v3.oas.annotations.tags` not on classpath.
2. **`doOnSuccess` null guard** in `LlmRoutingService` — when `LlmClient` returns `Mono.empty()`, `doOnSuccess` fires with `response=null`; NPE was caught by `onErrorMap` and wrapped as `AiProviderException`. Added `if (response == null) return;` guard.

---

## ArchUnit Rules (5)

1. Domain has no Spring dependencies
2. Domain has no infrastructure dependencies
3. Application has no infrastructure dependencies
4. Application has no API dependencies
5. Infrastructure/API do not access domain internals directly (port discipline)

---

---

## Addendum — 2026-03-10: OpenRouter Integration

**Status:** EXTENDED (tests: 11/11, BUILD SUCCESS)

### New Files Added

| File | Layer | Purpose |
|---|---|---|
| `infrastructure/config/OpenRouterProperties.java` | Infrastructure | `@ConfigurationProperties(prefix = "openrouter")` record — apiKey, baseUrl, model, timeouts |
| `infrastructure/external/OpenRouterWebClientAdapter.java` | Infrastructure | `LlmClient` adapter; calls OpenRouter's OpenAI-compatible `/api/v1/chat/completions`; `@Component("openRouterLlmClient")` |

### Modified Files

| File | Change |
|---|---|
| `domain/model/LlmProvider.java` | Added `OPENROUTER` enum constant |
| `infrastructure/external/AnthropicWebClientAdapter.java` | Added `@Primary` (preserves default bean); added `OPENROUTER` switch case with error guard |
| `application/service/LlmRoutingService.java` | Constructor-injected `@Qualifier("openRouterLlmClient")`; OPENROUTER case routes to OpenRouter adapter |
| `resources/application.yml` | Added `openrouter:` block with `${ENV_VAR:default}` pattern |
| `.env` | Added 5 `OPENROUTER_*` vars (key: `or-dev-placeholder` for local-echo mode) |
| `.env.example` | Added OpenRouter section |

### Local-Echo Fallback
`OpenRouterWebClientAdapter.isPlaceholderKey()` returns `true` when `OPENROUTER_API_KEY` starts with `or-dev` or is blank — identical keyword-matching echo logic as Anthropic adapter. All AI features remain functional without a real API key.

### Environment Variables
```
OPENROUTER_API_KEY=or-dev-placeholder
OPENROUTER_BASE_URL=https://openrouter.ai
OPENROUTER_MODEL=openai/gpt-4o-mini
OPENROUTER_CONNECT_TIMEOUT_MS=5000
OPENROUTER_READ_TIMEOUT_MS=30000
```
