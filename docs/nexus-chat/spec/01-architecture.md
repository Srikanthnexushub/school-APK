# NexusChat — Architecture Specification

## 1. System Topology

```
Browser (All Roles: STUDENT / PARENT / TEACHER / CENTER_ADMIN)
    │
    │  HTTP/SSE  →  nginx :80  →  api-gateway :8180
    │
    ├── POST /api/v1/chat/sessions            → nexus-chat-svc :8097
    ├── POST /api/v1/chat/sessions/{id}/stream (SSE)  → nexus-chat-svc :8097
    ├── GET  /api/v1/chat/sessions            → nexus-chat-svc :8097
    └── GET  /api/v1/chat/nudges/pending      → nexus-chat-svc :8097

nexus-chat-svc :8097
    │
    ├── Context Fan-out (parallel, ≤800ms total)
    │   ├── student-profile-svc :8090  →  GET /api/v1/students/{id}
    │   ├── performance-svc     :8092  →  GET /api/v1/performance/readiness/{id}
    │   │                              →  GET /api/v1/performance/mastery/{id}
    │   │                              →  GET /api/v1/performance/weak-areas/{id}
    │   ├── ai-mentor-svc       :8093  →  GET /api/v1/study-plans?studentId={id}
    │   │                              →  GET /api/v1/doubts?studentId={id}
    │   ├── assess-svc          :8084  →  GET /api/v1/exams/submissions/latest?studentId={id}
    │   └── center-svc          :8083  →  GET /api/v1/centers/student-enrollment/me
    │
    ├── AI Streaming
    │   └── ai-gateway-svc :8086  →  POST /api/v1/ai/completions/stream (NEW)
    │
    ├── Action Execution
    │   ├── ai-mentor-svc :8093  →  POST /api/v1/study-plans
    │   ├── ai-mentor-svc :8093  →  POST /api/v1/reminders
    │   └── navigate (frontend-only, returned in response)
    │
    └── Kafka
        ├── CONSUMER: assess-events          (ExamSubmittedEvent)
        ├── CONSUMER: performance-events     (WeakAreaDetectedEvent)
        ├── CONSUMER: ai-mentor-study-plan-created
        ├── PRODUCER: nexus-chat-events      (audit)
        └── PRODUCER: notification-send      (proactive nudges)

chat_schema (PostgreSQL — same DB instance, own schema)
    ├── chat_sessions
    ├── chat_messages
    ├── context_snapshots
    └── proactive_nudges
```

## 2. Request Lifecycle — Send Message (Streaming)

```
1. Frontend: POST /api/v1/chat/sessions/{id}/stream
   Body: { message: "Why is my ERS score low?", pageContext: "performance" }
   Headers: Authorization: Bearer {jwt}

2. ChatController receives → extracts X-User-Id, X-User-Role from gateway headers

3. ChatSessionService.processStream(sessionId, message, userId):
   a. Load session + last N messages from chat_schema (N = CHAT_MAX_HISTORY_MESSAGES)
   b. Load ContextSnapshot for session (cached at session-start, refreshed if >15min old)
   c. Build system prompt via SystemPromptBuilder.build(context, pageContext)
   d. Append conversation history as messages array
   e. Call AiGatewayStreamingAdapter.stream(systemPrompt, history, userMessage)
   f. Return Flux<String> of tokens

4. ChatController wraps Flux<String> as:
   - Content-Type: text/event-stream
   - Each token: "data: {\"token\":\"Why\",\"done\":false}\n\n"
   - Final:        "data: {\"token\":\"\",\"done\":true,\"sessionId\":\"...\"}\n\n"

5. After stream completes (doOnComplete):
   - Persist user message to chat_messages
   - Persist assistant full message to chat_messages
   - Update chat_session.last_active_at + message_count
   - Check streamed content for ACTION JSON block
   - If ACTION found: publish to Kafka nexus-chat-events (audit)

6. Frontend ReadableStream reader:
   - Appends each token to streamingContent in chatStore
   - On done: calls finalizeStream(), saves message to store
   - If actionJson present: calls ActionExecutorService (frontend-side navigation)
     OR sends POST /api/v1/chat/sessions/{id}/actions for server-side actions
```

## 3. Proactive Nudge Lifecycle

```
assess-svc publishes ExamSubmittedEvent to Kafka topic: assess-events

nexus-chat-svc:ProactiveNudgeKafkaConsumer receives event
    → NudgeFactory.createFromExamEvent(event):
        - Fetches submission details from assess-svc
        - Generates nudge message: "You just finished {exam}! Score: {score}%. 
          Want me to debrief the {wrongCount} questions you got wrong?"
        - Saves to chat_schema.proactive_nudges
        - Publishes NotificationSendEvent to notification-send topic:
            subject: "NexusChat has insights for you"
            body: nudge message
            actionUrl: "/chat?nudge={nudgeId}"
            notificationType: "NEXUS_CHAT_NUDGE"

notification-svc receives → pushes via SSE to student's browser
    → Bell icon shows red dot
    → Student clicks → opens NexusChatWidget with pre-loaded nudge context
```

## 4. Context Aggregation — Timing Budget

```
Total budget: 800ms (CHAT_CONTEXT_TOTAL_TIMEOUT_MS)

Parallel calls (all fire at T=0):
  student-profile-svc  →  timeout 500ms  → cached 15min
  performance-svc      →  timeout 500ms  → cached 5min (changes after exams)
  ai-mentor-svc        →  timeout 500ms  → cached 10min
  assess-svc           →  timeout 500ms  → cached 10min
  center-svc           →  timeout 500ms  → cached 30min

Each call has onErrorReturn(EmptyDto) — no call can block the chat.
Mono.zip waits max 800ms then assembles StudentContext from whatever returned.
```

## 5. Streaming Architecture — Tomcat WAR Constraint

```
CONSTRAINT: nexus-chat-svc is a Tomcat WAR (not Netty exec JAR).
Spring WebFlux Flux<String> requires Netty for true reactive streaming.

SOLUTION: Use Spring MVC ResponseBodyEmitter on Tomcat.
  - ChatController uses ResponseEntity<ResponseBodyEmitter>
  - AiGatewayStreamingAdapter calls ai-gateway-svc/stream endpoint
  - Receives chunked HTTP response via RestTemplate with ResponseExtractor
  - Writes each chunk to ResponseBodyEmitter on a dedicated thread pool
  - Thread pool size: CHAT_STREAM_THREAD_POOL_SIZE (default 10)

ai-gateway-svc is an exec WAR (standalone Netty) — CAN use WebFlux SSE.
  → Add streaming endpoint there: POST /api/v1/ai/completions/stream
  → Returns text/event-stream via Flux<ServerSentEvent<String>>
  → OpenRouter/Anthropic both support streaming (stream: true)

Frontend connects directly to:
  POST /api/v1/chat/sessions/{id}/stream (nexus-chat-svc via Tomcat)
  Content-Type: text/event-stream
  Uses ResponseBodyEmitter pattern (NOT WebFlux SSE)
```

## 6. Role-Based Behavior

| Role | System Prompt Focus | Available Actions | Quick Chips |
|---|---|---|---|
| STUDENT | Academic performance, weak areas, exam prep | CREATE_STUDY_PLAN, SCHEDULE_REMINDER, NAVIGATE | Performance, Exam, Study |
| PARENT | Child's progress, fees, upcoming exams | SHOW_CHILD_PERFORMANCE, SHOW_FEES, NAVIGATE | Child progress, Fees, Attendance |
| TEACHER | Class performance, batch analytics, exam creation | SHOW_BATCH_PERFORMANCE, NAVIGATE | Class stats, Exam results |
| CENTER_ADMIN | Enrollment stats, fee collection, teacher mgmt | SHOW_ENROLLMENT_STATS, NAVIGATE | Revenue, Attendance rate |

## 7. Files Changed in Existing Services

### ai-gateway-svc (2 files modified, 0 deleted)
- `CompletionController.java` → add `/stream` endpoint
- `OpenRouterWebClientAdapter.java` → add `streamCompletion()` method
- `AnthropicWebClientAdapter.java` → add `streamCompletion()` method
- `LlmRoutingService.java` → add `streamCompletion()` routing method
- `LlmClient.java` (interface) → add `streamCompletion()` method signature
- `application.yml` → no changes needed

### api-gateway (1 file modified)
- `application.yml` → add nexus-chat-svc route before existing routes

### frontend/web (1 file modified + new files)
- `AppLayout.tsx` → add `<NexusChatWidget pageContext={pageContext} />` (1 line)
- All other changes are NEW files in `components/chat/`, `hooks/`, `stores/`

### .env (additions only, never modify existing)
- Add NEXUS_CHAT_SVC_PORT, NEXUS_CHAT_SVC_URI, all new env vars

### root pom.xml
- Add `<module>services/nexus-chat-svc</module>`
