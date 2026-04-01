# NexusChat — Implementation Waves Checklist

## Wave 0 — Infrastructure Setup (Prerequisites)
- [ ] Create PostgreSQL DB + user (`nexus_chat_db` / `nexus_chat_user`)
- [ ] Append `.env` entries (10 new lines — see spec/12)
- [ ] Add `<module>services/nexus-chat-svc</module>` to root `pom.xml`
- [ ] Add route to `api-gateway/application.yml`
- [ ] Add route to `student-gateway/application.yml`
- [ ] Add `start_svc "nexus-chat-svc" "8097" "war"` to `start-all.sh`

---

## Wave 1 — nexus-chat-svc Skeleton + DB Migrations

### Create service directory structure
```
services/nexus-chat-svc/
  pom.xml
  src/main/
    java/com/edutech/chat/
      NexusChatApplication.java
      domain/
        model/         (ChatSession, ChatMessage, ProactiveNudge, enums, VOs)
        port/
          out/         (ChatSessionRepository, ChatMessageRepository, AiGatewayStreamPort, StreamTokenConsumer)
      application/
        service/       (ChatSessionService, ContextAggregatorService, SystemPromptBuilder, ProactiveNudgeService)
      infrastructure/
        persistence/   (JpaChatSessionRepository, JpaChatMessageRepository, SpringDataChatSessionRepo, SpringDataChatMessageRepo)
        ai/            (AiGatewayStreamWebClientAdapter)
        config/        (WebClientConfig, StreamThreadPoolConfig, SecurityConfig)
        kafka/         (PlatformEventKafkaConsumer, KafkaChatEventPublisher)
        api/
          controller/  (ChatController)
          dto/         (all request/response DTOs)
    resources/
      application.yml
      db/migration/chat/
        V1__create_chat_schema.sql
        V2__create_chat_sessions.sql
        V3__create_chat_messages.sql
        V4__create_context_snapshots.sql
        V5__create_proactive_nudges.sql
```

### Files to create (Wave 1)
- [ ] `pom.xml` (from spec/02)
- [ ] `application.yml` (from spec/02)
- [ ] `NexusChatApplication.java` (from spec/02)
- [ ] All 5 SQL migration files (from spec/03)
- [ ] Domain models: `ChatSession.java`, `ChatMessage.java`, `ProactiveNudge.java` (from spec/04)
- [ ] Enums: `MessageRole`, `MessageType`, `SessionStatus`, `NudgeTriggerType` (from spec/04)
- [ ] Value objects: `StudentContext.java` + nested records (from spec/04)
- [ ] `ActionCommand.java` (from spec/04)
- [ ] Port interfaces: `ChatSessionRepository`, `ChatMessageRepository`, `AiGatewayStreamPort`, `StreamTokenConsumer` (from spec/04)

### Verify Wave 1
```bash
mvn clean compile -pl services/nexus-chat-svc -am -DskipTests
```
Expected: BUILD SUCCESS (domain compiles, no Spring context needed yet)

---

## Wave 2 — Application Services

### Files to create (Wave 2)
- [ ] `ChatSessionService.java` (from spec/05)
- [ ] `ContextAggregatorService.java` (from spec/05)
- [ ] `SystemPromptBuilder.java` (from spec/05)
- [ ] `ProactiveNudgeService.java` (from spec/05)

### Verify Wave 2
```bash
mvn clean compile -pl services/nexus-chat-svc -am -DskipTests
```

---

## Wave 3 — Infrastructure Layer

### Files to create (Wave 3)
- [ ] `ChatController.java` (from spec/06)
- [ ] All DTO records in `api/dto/` (from spec/06)
- [ ] `JpaChatSessionRepository.java` + `SpringDataChatSessionRepo.java` (from spec/06)
- [ ] `JpaChatMessageRepository.java` + `SpringDataChatMessageRepo.java` (from spec/06)
- [ ] `AiGatewayStreamWebClientAdapter.java` (from spec/06)
- [ ] `WebClientConfig.java` (from spec/06)
- [ ] `StreamThreadPoolConfig.java` (from spec/06)
- [ ] `SecurityConfig.java` (from spec/06)
- [ ] `PlatformEventKafkaConsumer.java` (from spec/06)
- [ ] `KafkaChatEventPublisher.java` (from spec/06)

### Verify Wave 3
```bash
mvn clean package -pl services/nexus-chat-svc -am -DskipTests
# Expected WAR: services/nexus-chat-svc/target/nexus-chat-svc-1.0.0-SNAPSHOT.war
```

---

## Wave 4 — Modify ai-gateway-svc (streaming support)

### Files to modify (Wave 4)
- [ ] `LlmClient.java` — add `streamCompletion()` to interface (from spec/07)
- [ ] `StreamCompletionRequest.java` — NEW domain model (from spec/07)
- [ ] `StreamCompletionRequestDto.java` — NEW API DTO (from spec/07)
- [ ] `OpenRouterWebClientAdapter.java` — add `streamCompletion()` + local echo (from spec/07)
- [ ] `AnthropicWebClientAdapter.java` — add `streamCompletion()` + local echo (from spec/07)
- [ ] `LlmRoutingService.java` — add `streamCompletion()` routing (from spec/07)
- [ ] `CompletionController.java` — add `/completions/stream` endpoint (from spec/07)

### Verify Wave 4
```bash
mvn clean package -pl services/ai-gateway-svc -am -DskipTests
```

---

## Wave 5 — Integration Test

### Startup
```bash
bash scripts/start-all.sh --no-build
# Wait for nexus-chat-svc on port 8097
curl http://localhost:8097/actuator/health
```

### Flyway migration check
```bash
# Connect to nexus_chat_db and verify tables
psql -U nexus_chat_user -d nexus_chat_db -c "\dt chat_schema.*"
# Expected: chat_sessions, chat_messages, context_snapshots, proactive_nudges
```

### API smoke tests (use studentA JWT)
```bash
# Get JWT
TOKEN=$(curl -s -X POST http://localhost:8180/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"studentA@school.com","password":"Test@12345","captchaToken":"E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD:bypass"}' \
  | jq -r '.token')

# 1. Start session
SESSION=$(curl -s -X POST http://localhost:8180/api/v1/chat/sessions \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"pageContext":"dashboard"}' | jq -r '.sessionId')
echo "Session: $SESSION"

# 2. Stream a message
curl -N http://localhost:8180/api/v1/chat/sessions/$SESSION/stream \
  -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Accept: text/event-stream' \
  -d '{"message":"What are my weak areas?","pageContext":"dashboard"}'
# Expected: SSE token stream ending with data: [DONE]
```

---

## Wave 6 — Frontend Components

### Files to create (Wave 6)
- [ ] `frontend/web/src/types/chat.ts` (from spec/10)
- [ ] `frontend/web/src/store/chatStore.ts` (from spec/10)
- [ ] `frontend/web/src/hooks/useNudgePoller.ts` (from spec/10)
- [ ] `frontend/web/src/components/chat/NexusChatWidget.tsx` (from spec/09)
- [ ] `frontend/web/src/components/chat/ChatPanel.tsx` (from spec/09)
- [ ] `frontend/web/src/components/chat/ChatMessageBubble.tsx` (from spec/09)
- [ ] `frontend/web/src/components/chat/StreamingMessage.tsx` (from spec/09)
- [ ] `frontend/web/src/components/chat/TypingIndicator.tsx` (from spec/09)
- [ ] `frontend/web/src/components/chat/ActionResultCard.tsx` (from spec/09)
- [ ] `frontend/web/src/components/chat/ContextChips.tsx` (from spec/09)

### Modify AppLayout.tsx (Wave 6)
- [ ] Add `NexusChatWidget` import
- [ ] Add `useNudgePoller` import
- [ ] Add `useNudgePoller()` call inside component
- [ ] Add `<NexusChatWidget />` before closing div

### Verify Wave 6
```bash
cd frontend/web && npx vite build
# Expected: BUILD SUCCESS, no TypeScript errors
```

---

## Wave 7 — End-to-End Verification

### Checklist
- [ ] Open http://localhost:3000, log in as studentA
- [ ] Chat button visible in bottom-right corner
- [ ] Click to open — greeting message appears
- [ ] Type "What are my weak areas?" — streaming response with tokens
- [ ] AI response references real student data (ERS score, subjects)
- [ ] Action card appears (SHOW_WEAK_AREAS) — clicking navigates to /performance
- [ ] Open second session — greeting adapts to page context
- [ ] Submit an exam → nudge bell badge appears within 60 seconds
- [ ] Click nudge → opens chat with context

---

## Wave 8 — EC2 Deployment

### Steps
1. Build all changed services:
   ```bash
   mvn clean package -DskipTests -T 4 -Drevision=1.0.0-PROD \
     -pl services/nexus-chat-svc,services/ai-gateway-svc,services/api-gateway,services/student-gateway -am
   ```

2. Create DB on RDS (one-time):
   ```bash
   ssh -i ~/.ssh/edutech-key.pem -o KexAlgorithms=ecdh-sha2-nistp256 ec2-user@13.126.138.9
   # Run SQL from spec/03 PostgreSQL setup section against RDS
   ```

3. Deploy WARs (nexus-chat-svc + ai-gateway-svc) + JARs (api-gateway, student-gateway)

4. Update EC2 .env with new entries from spec/12

5. Add Tomcat instance for nexus-chat-svc (new port 8097)

6. Update nginx upstream if needed

7. Verify: `curl http://13.126.138.9/api/v1/chat/sessions`

---

## Known Constraints & Gotchas

1. **nexus-chat-svc is a Tomcat WAR** — uses `ResponseBodyEmitter` NOT `Flux<>`. Do NOT add `spring-boot-starter-webflux` as server (only as WebClient dependency).
2. **WebClient in Tomcat WAR** — requires `reactor-netty` on classpath but NOT as the server. The pom.xml in spec/02 handles this correctly.
3. **JWT validation** — `common-security` module is a dependency. The RSA public key path must be set in application.yml.
4. **Context aggregation timeout** — `Mono.zip` with 800ms total. Each individual call has its own `timeout(Duration.ofMillis(600))`. If student-profile-svc is slow, `StudentContext.empty()` is returned — chat still works.
5. **Rate limiting** — 30 messages/hour per user enforced in `ChatSessionService.streamMessage()`. Stored in Redis (same Redis used by other services).
6. **Local dev echo** — if `AI_GATEWAY_SERVICE_KEY` is missing from .env, `AiGatewayStreamWebClientAdapter` falls back to word-split echo simulation at 50ms/word.
