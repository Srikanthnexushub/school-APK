# EduTech AI Platform — Service Dependency Map

> **Document status:** Living document — updated at each service milestone.
> **Last updated:** 2026-03-07
> **Platform version:** 1.0.0-SNAPSHOT

---

## Table of Contents

1. [Dependency Matrix](#1-dependency-matrix)
2. [Per-Service Dependencies](#2-per-service-dependencies)
3. [Startup Order](#3-startup-order)
4. [Failure Impact Analysis](#4-failure-impact-analysis)
5. [External Dependencies](#5-external-dependencies)

---

## 1. Dependency Matrix

The table below summarises what each service depends on. "X" indicates a hard runtime dependency; "proxy" means the gateway routes HTTP to that service without a direct application-layer dependency; "(P2)" indicates a Phase 2 planned dependency not yet active.

| Service | api-gateway | auth-svc | center-svc | parent-svc | assess-svc | psych-svc | ai-gateway-svc | psych-ai-svc | Anthropic API | OpenAI API | Ollama | hCaptcha API | SMTP | Twilio | Firebase | PostgreSQL | Redis | Kafka |
|---------|:-----------:|:--------:|:----------:|:----------:|:----------:|:---------:|:--------------:|:------------:|:-------------:|:----------:|:------:|:------------:|:----:|:------:|:--------:|:----------:|:-----:|:-----:|
| api-gateway | — | proxy | proxy | proxy | proxy | proxy | proxy | | | | | | | | | | X | |
| auth-svc | | — | | | | | | | | | | X | X | X | | X | X | X |
| center-svc | | | — | | | | X | | | | | | | | | X | X | X |
| parent-svc | | | | — | | | X | | | | | | | | X | X | X | X |
| assess-svc | | | | | — | | X | | | | | | | | | X | X | X |
| psych-svc | | | | | | — | X | X | | | | | | | | X | X | X |
| ai-gateway-svc | | | | | | | — | X | X | X | X(P2) | | | | | | X | X |

**Kafka topic flows** (separate from HTTP):

| Topic | Producer | Consumer(s) |
|-------|---------|------------|
| `auth-events` | auth-svc | (future: notification-svc) |
| `center-events` | center-svc | parent-svc, assess-svc |
| `parent-events` | parent-svc | (future: notification-svc) |
| `assess-events` | assess-svc | psych-svc |
| `psych-events` | psych-svc | (future: analytics-svc, notify-svc) |
| `ai-gateway-events` | ai-gateway-svc | (future: cost-analytics-svc) |
| `audit-immutable` | all services | (future: audit-svc — read-only) |
| `notification-send` | auth-svc | (future: notification-svc) |

---

## 2. Per-Service Dependencies

### 2.1 api-gateway

**Purpose:** Edge proxy. Routes all inbound client traffic to downstream services. Enforces JWT RS256 authentication via a `GlobalFilter` and applies Redis token-bucket rate limiting to all requests.

**Inbound:** External clients (web browsers, mobile apps, API consumers).

**Outbound HTTP (proxy routes):**

| Route ID | Path Predicate | Destination Env Var |
|----------|---------------|---------------------|
| `auth-svc` | `/api/v1/auth/**` | `${AUTH_SVC_URI}` |
| `center-svc` | `/api/v1/centers/**` | `${CENTER_SVC_URI}` |
| `parent-svc` | `/api/v1/parents/**` | `${PARENT_SVC_URI}` |
| `assess-svc` | `/api/v1/assessments/**` | `${ASSESS_SVC_URI}` |
| `psych-svc` | `/api/v1/psych/**` | `${PSYCH_SVC_URI}` |
| `ai-gateway-svc` | `/api/v1/ai/**` | `${AI_GATEWAY_SVC_URI}` |

Public paths that bypass JWT validation: `/api/v1/auth/**`, `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/webjars/**`

Headers injected on valid JWT and forwarded to all downstream services: `X-User-Id`, `X-User-Role`, `X-User-Center-Id`, `X-Request-ID`

**Outbound async (Kafka):** None. The gateway publishes no Kafka events.

**Database:** None. The gateway is fully stateless — no JPA, no datasource configured.

**Cache (Redis):**
- Spring Cloud Gateway `RequestRateLimiter` default filter (Redis token bucket).
- Configuration: `redis-rate-limiter.replenish-rate`, `redis-rate-limiter.burst-capacity`, `redis-rate-limiter.requested-tokens` — all via env vars.
- Behaviour on Redis outage: rate limiting **fails open** — requests pass through unmetered. By design.

**Resilience4j circuit breaker (auth-svc route):** `sliding-window-size: ${R4J_CB_AUTH_WINDOW_SIZE}`, `failure-rate-threshold: ${R4J_CB_AUTH_FAILURE_THRESHOLD}`, `wait-duration-in-open-state: ${R4J_CB_AUTH_WAIT_DURATION}`

**Runtime:** Spring Cloud Gateway (WebFlux-based reactive runtime). `GlobalFilter` order `-2` injects `X-Request-ID`; order `-1` enforces JWT.

---

### 2.2 auth-svc

**Purpose:** Authentication and identity boundary. The **only** service permitted to issue, rotate, and revoke JWT access tokens. All other services validate tokens independently but never produce them.

**Inbound:**
- `api-gateway` (proxies `/api/v1/auth/**`)
- No direct calls from other services (JWT validation is done locally in each service, not via auth-svc at runtime)

**Outbound HTTP (synchronous):**

| Target | Adapter Class | Call | Circuit Breaker |
|--------|--------------|------|-----------------|
| hCaptcha Enterprise API | `HCaptchaRestAdapter` (Spring `RestClient`) | `POST ${HCAPTCHA_VERIFY_URL}` with `secret`, `response` (captcha token), `remoteip` as form body | `captcha-client` — fallback: `return false` (fail closed — reject on outage) |
| SMTP server | `SmtpNotificationAdapter` (JavaMailSender) | Sends OTP emails for `EMAIL_VERIFICATION`, `PASSWORD_RESET`, `LOGIN_MFA` purposes | None — exception propagates as HTTP 500 |
| Twilio Verify API | `SmtpNotificationAdapter.sendOtpSms()` | SMS OTP delivery (production; dev logs OTP to console) | None |

**Outbound async (Kafka topics published):**

| Topic (resolved from env var) | Events Published | Trigger |
|-------------------------------|-----------------|---------|
| `${KAFKA_TOPIC_AUTH_EVENTS}` | User registered, authenticated, logged out, token refreshed, OTP requested | On each auth use case completion |
| `${KAFKA_TOPIC_AUDIT_IMMUTABLE}` | All auth events duplicated to the immutable audit log | On each auth use case completion |
| `${KAFKA_TOPIC_NOTIFICATION_SEND}` | Notification dispatch requests | On OTP send and account lifecycle events |

Kafka adapter: `AuditEventKafkaAdapter` (implements `AuditEventPublisher`). Fire-and-forget — publish failures are logged at `ERROR` but never roll back the primary DB transaction.

**Kafka consumed:** None in Phase 1.

**Database:** `auth_schema` (PostgreSQL)

| Table | Key Columns |
|-------|-------------|
| `auth_schema.users` | `id UUID PK`, `email TEXT UNIQUE`, `password_hash TEXT` (Argon2id), `role TEXT`, `center_id UUID`, `status TEXT`, `version BIGINT` (`@Version`), `deleted_at TIMESTAMPTZ` |

Flyway migrations path: `classpath:db/migration/auth`

**Cache (Redis):**

| Usage | Port Interface | Adapter Class | Key Pattern | TTL |
|-------|---------------|---------------|-------------|-----|
| Refresh token store | `TokenStore` | `RefreshTokenRedisAdapter` | `refresh:{tokenId}` | `${JWT_REFRESH_TOKEN_EXPIRY_SECONDS}` |
| OTP value | `OtpStore` | `OtpRedisAdapter` | `otp:{purpose}:{userId}` | `${OTP_EXPIRY_SECONDS}` |
| OTP attempt counter | `OtpStore.incrementAttempts()` | `OtpRedisAdapter` | `otp:attempts:{purpose}:{userId}` | Aligned with OTP TTL |

**Outbound ports → adapters:**

| Domain Port Interface | Infrastructure Adapter |
|-----------------------|------------------------|
| `UserRepository` | `UserPersistenceAdapter` → package-private `SpringDataUserRepository` |
| `PasswordHasher` | `Argon2PasswordHasherAdapter` (Argon2id, all parameters externalized) |
| `AccessTokenGenerator` | `JwtTokenProvider` (JJWT 0.12.x RS256 signing with `${JWT_PRIVATE_KEY_PATH}`) |
| `TokenStore` | `RefreshTokenRedisAdapter` |
| `OtpStore` | `OtpRedisAdapter` |
| `AuditEventPublisher` | `AuditEventKafkaAdapter` |
| `CaptchaVerifier` | `HCaptchaRestAdapter` |
| `NotificationSender` | `SmtpNotificationAdapter` |

---

### 2.3 center-svc

**Purpose:** Operational backbone. Owns all physical and academic infrastructure: coaching centres, teacher rosters, batch schedules, fee structures, attendance records, and study content items.

**Inbound:**
- `api-gateway` (proxies `/api/v1/centers/**`)

**Outbound HTTP (synchronous):**

| Target | Config Key | Use | Circuit Breaker |
|--------|-----------|-----|-----------------|
| `ai-gateway-svc` | `${AI_GATEWAY_BASE_URL}` | AI completion requests for content assistance and curriculum suggestions | `ai-gateway` (Resilience4j) |

**Outbound async (Kafka topics published):**

| Topic (env var) | Events | Trigger |
|-----------------|--------|---------|
| `${KAFKA_TOPIC_CENTER_EVENTS}` | `CenterCreatedEvent`, `BatchStatusChangedEvent`, `TeacherAssignedEvent`, `ScheduleChangedEvent`, `ContentUploadedEvent` | On each center/batch/content domain operation |
| `${KAFKA_TOPIC_AUDIT_IMMUTABLE}` | All center events duplicated to audit | On all domain operations |

**Kafka consumed:** None in Phase 1.

**Database:** `center_schema` (PostgreSQL)

| Table | Purpose |
|-------|---------|
| `center_schema.centers` | Center master data |
| `center_schema.teachers` | Teacher roster per center |
| `center_schema.batches` | Academic batch definitions and lifecycle state |
| `center_schema.schedules` | Batch session schedules |
| `center_schema.fee_structures` | Fee plans per batch/center |
| `center_schema.attendance_records` | Per-student attendance per session |
| `center_schema.content_items` | Study material and content uploads |

Flyway migrations path: `classpath:db/migration/center`

**Cache (Redis):** Redis connection is configured in `application.yml` but no center-specific caching is active in Phase 1.

**Outbound ports → adapters:**

| Domain Port Interface | Infrastructure Adapter |
|-----------------------|------------------------|
| `CenterRepository` | `CenterPersistenceAdapter` |
| `BatchRepository` | `BatchPersistenceAdapter` |
| `TeacherRepository` | `TeacherPersistenceAdapter` |
| `ScheduleRepository` | `SchedulePersistenceAdapter` |
| `FeeStructureRepository` | `FeeStructurePersistenceAdapter` |
| `AttendanceRepository` | `AttendancePersistenceAdapter` |
| `ContentRepository` | `ContentPersistenceAdapter` |
| `CenterEventPublisher` | Kafka adapter → `center-events` + `audit-immutable` |

---

### 2.4 parent-svc

**Purpose:** Parent-facing bounded context. Manages parent profiles, student-to-parent links, fee payment records, notification preferences, and a "Parent Copilot" AI assistant feature.

**Inbound:**
- `api-gateway` (proxies `/api/v1/parents/**`)

**Outbound HTTP (synchronous):**

| Target | Config Key | Use | Circuit Breaker |
|--------|-----------|-----|-----------------|
| `ai-gateway-svc` | `${AI_GATEWAY_BASE_URL}` | Parent Copilot AI completion requests (progress summaries, recommendations) | `ai-gateway` (Resilience4j) |

**Outbound async (Kafka topics published):**

| Topic (env var) | Events | Trigger |
|-----------------|--------|---------|
| `${KAFKA_TOPIC_PARENT_EVENTS}` | `StudentLinkedEvent`, `LinkRevokedEvent`, `FeePaymentRecordedEvent` | On parent domain operations |
| `${KAFKA_TOPIC_AUDIT_IMMUTABLE}` | All parent events duplicated to audit | On all domain operations |

Adapter: `ParentEventKafkaAdapter` (implements `ParentEventPublisher`). Fire-and-forget, publishes to `topicProperties.parentEvents()`.

**Kafka consumed:**

| Topic (env var) | Consumer Class | Phase 1 Behaviour | Phase 2 Intent |
|-----------------|---------------|-------------------|----------------|
| `${KAFKA_TOPIC_CENTER_EVENTS}` | `CenterEventConsumer` | Logs raw event payload (`log.info`) | Trigger push notifications to parents on `BatchStatusChangedEvent`, `ContentUploadedEvent` |

Consumer group: `${PARENT_SVC_KAFKA_CONSUMER_GROUP}`, `auto-offset-reset: earliest`

**Database:** `parent_schema` (PostgreSQL)

| Table | Purpose |
|-------|---------|
| `parent_schema.parent_profiles` | Parent identity and profile data |
| `parent_schema.student_links` | Parent → student UUID cross-references |
| `parent_schema.fee_payments` | Fee payment records and status |
| `parent_schema.notification_preferences` | Per-parent notification channel settings |

Flyway migrations path: `classpath:db/migration/parent`

**Cache (Redis):** Connected but no parent-specific caching in Phase 1.

**External (non-service):**
- Firebase FCM (`${FIREBASE_PROJECT_ID}`, `${FIREBASE_SERVICE_ACCOUNT_KEY_PATH}`): mobile push notifications to parent app. Used exclusively by `parent-svc`.

**Outbound ports → adapters:**

| Domain Port Interface | Infrastructure Adapter |
|-----------------------|------------------------|
| `ParentProfileRepository` | `ParentProfilePersistenceAdapter` |
| `StudentLinkRepository` | `StudentLinkPersistenceAdapter` |
| `FeePaymentRepository` | `FeePaymentPersistenceAdapter` |
| `NotificationPreferenceRepository` | `NotificationPreferencePersistenceAdapter` |
| `ParentEventPublisher` | `ParentEventKafkaAdapter` → `parent-events` + `audit-immutable` |

---

### 2.5 assess-svc

**Purpose:** Assessment and grading bounded context. Owns exams, question banks with vector embeddings, student enrolments, exam submission lifecycle, AI-assisted grading, and Computer Adaptive Testing (CAT) using Item Response Theory. Exposes a STOMP WebSocket relay for real-time exam sessions.

**Inbound:**
- `api-gateway` (proxies `/api/v1/assessments/**`)
- STOMP WebSocket connections from student clients during live exam sessions

**Outbound HTTP (synchronous):**

| Target | Config Key | Use | Circuit Breaker |
|--------|-----------|-----|-----------------|
| `ai-gateway-svc` | `${AI_GATEWAY_BASE_URL}` | AI-assisted grading of open-ended answers, question generation, embedding requests | `ai-gateway` (Resilience4j) |

**Outbound async (Kafka topics published):**

| Topic (env var) | Events | Trigger |
|-----------------|--------|---------|
| `${KAFKA_TOPIC_ASSESS_EVENTS}` | `ExamPublishedEvent`, `ExamSubmittedEvent`, `GradeIssuedEvent` | On exam/submission/grade domain operations |
| `${KAFKA_TOPIC_AUDIT_IMMUTABLE}` | All assess events duplicated to audit | On all domain operations |

Adapter: `AssessEventKafkaAdapter` (implements `AssessEventPublisher`). Fire-and-forget, publishes to `topicProperties.assessEvents()`.

**Kafka consumed:**

| Topic (env var) | Consumer Class | Phase 1 Behaviour | Phase 2 Intent |
|-----------------|---------------|-------------------|----------------|
| `${KAFKA_TOPIC_CENTER_EVENTS}` | `CenterEventConsumer` | Logs raw event payload | Invalidate in-progress submissions for batches that close (`BatchStatusChangedEvent`) |

Consumer group: `${ASSESS_SVC_KAFKA_CONSUMER_GROUP}`, `auto-offset-reset: earliest`

**Database:** `assess_schema` (PostgreSQL)

| Table | Purpose |
|-------|---------|
| `assess_schema.exams` | Exam definitions (title, type, batch association, schedule) |
| `assess_schema.questions` | Question bank — includes `embedding_json TEXT` for similarity search (Phase 2: `vector(1536)`) |
| `assess_schema.exam_enrollments` | Student exam enrolment records |
| `assess_schema.submissions` | Exam session and submission lifecycle state machine |
| `assess_schema.submission_answers` | Per-question student answers |
| `assess_schema.grades` | Computed grades per submission |

Flyway migrations path: `classpath:db/migration/assess` (V1–V7, 7 migration scripts)

pgvector embedding dimensions: `${AI_EMBEDDING_DIMENSIONS}`

CAT engine configuration (all externalized): `${CAT_MIN_QUESTIONS}`, `${CAT_MAX_QUESTIONS}`, `${CAT_INITIAL_THETA}`, `${CAT_CONVERGENCE_THRESHOLD}`

**Cache (Redis):** Connected but no assess-specific caching in Phase 1.

**Outbound ports → adapters:**

| Domain Port Interface | Infrastructure Adapter |
|-----------------------|------------------------|
| `ExamRepository` | `ExamPersistenceAdapter` |
| `QuestionRepository` | `QuestionPersistenceAdapter` |
| `ExamEnrollmentRepository` | `ExamEnrollmentPersistenceAdapter` |
| `SubmissionRepository` | `SubmissionPersistenceAdapter` |
| `SubmissionAnswerRepository` | `SubmissionAnswerPersistenceAdapter` |
| `GradeRepository` | `GradePersistenceAdapter` |
| `AssessEventPublisher` | `AssessEventKafkaAdapter` → `assess-events` + `audit-immutable` |

---

### 2.6 psych-svc

**Purpose:** Psychometric profiling bounded context. Owns student psychometric profiles (Big Five: Openness, Conscientiousness, Extraversion, Agreeableness, Neuroticism + RIASEC career type code), assessment sessions, and career mapping recommendations generated by the Python AI sidecar.

**Inbound:**
- `api-gateway` (proxies `/api/v1/psych/**`)

**Outbound HTTP (synchronous):**

| Target | Adapter Class | Call | Circuit Breaker | Failure Mode |
|--------|--------------|------|-----------------|--------------|
| `psych-ai-svc` (Python FastAPI sidecar) | `PsychAiSvcWebClientAdapter` (WebClient + `.block()`) | `POST ${PSYCH_AI_SVC_BASE_URL}/api/v1/predict-careers` with Big Five scores + RIASEC code | `psych-ai-svc` (Resilience4j) | CB open → `RuntimeException` thrown → `CareerMappingService` catches → `CareerMapping` saved as `FAILED` → HTTP 201 returned |
| `ai-gateway-svc` | WebClient | `${AI_GATEWAY_BASE_URL}` — AI completion requests (Phase 2: session transcript analysis) | `ai-gateway` (Resilience4j) | `AiProviderException` → HTTP 502 |

**Request to psych-ai-svc:**
```
profileId: String (UUID)
openness, conscientiousness, extraversion, agreeableness, neuroticism: double (0.0–1.0)
riasecCode: String (e.g. "RIA")
```

**Response from psych-ai-svc:**
```
topCareers: List<String>
reasoning: String
modelVersion: String
```

**Outbound async (Kafka topics published):**

| Topic (env var) | Events | Key | Trigger |
|-----------------|--------|-----|---------|
| `${KAFKA_TOPIC_PSYCH_EVENTS}` | `PsychProfileCreatedEvent`, `SessionCompletedEvent`, `CareerMappingGeneratedEvent` | entity UUID | On each corresponding domain operation |
| `${KAFKA_TOPIC_AUDIT_IMMUTABLE}` | All psych events duplicated to audit | entity UUID | On all domain operations |

Adapter: `PsychEventKafkaAdapter` (implements `PsychEventPublisher`). Dispatches each event type to both topics by key. Exceptions caught and logged (best-effort).

**Kafka consumed:**

| Topic (env var) | Consumer Class | Phase 1 Behaviour | Phase 2 Intent |
|-----------------|---------------|-------------------|----------------|
| `${KAFKA_TOPIC_ASSESS_EVENTS}` | `AssessEventConsumer` | Logs raw event payload | Trigger a `TRIGGERED` type session when grade patterns indicate academic stress |

Consumer group: `${PSYCH_SVC_KAFKA_CONSUMER_GROUP}`, `auto-offset-reset: earliest`

**Database:** `psych_schema` (PostgreSQL)

| Table | Purpose |
|-------|---------|
| `psych_schema.trait_dimensions` | Reference data — 11 seeded rows (5 Big Five + 6 RIASEC). No soft delete. No `@Version`. |
| `psych_schema.psych_profiles` | Core aggregate — Big Five scores, RIASEC code, `embedding_json TEXT` (Phase 2: pgvector), `UNIQUE(student_id, center_id)` |
| `psych_schema.session_histories` | Assessment session lifecycle: SCHEDULED → IN_PROGRESS → COMPLETED / CANCELLED |
| `psych_schema.career_mappings` | AI career predictions: PENDING → GENERATED / FAILED |

Flyway migrations path: `classpath:db/migration/psych` (V1–V5; V2 inserts 11 reference rows into `trait_dimensions`)

**Cache (Redis):** Redis connection is configured but no psych-specific caching is active in Phase 1.

**Outbound ports → adapters:**

| Domain Port Interface | Infrastructure Adapter |
|-----------------------|------------------------|
| `PsychProfileRepository` | `PsychProfilePersistenceAdapter` |
| `TraitDimensionRepository` | `TraitDimensionPersistenceAdapter` |
| `SessionHistoryRepository` | `SessionHistoryPersistenceAdapter` |
| `CareerMappingRepository` | `CareerMappingPersistenceAdapter` |
| `PsychEventPublisher` | `PsychEventKafkaAdapter` |
| `PsychAiSvcClient` | `PsychAiSvcWebClientAdapter` |

---

### 2.7 ai-gateway-svc

**Purpose:** Fully reactive AI gateway (Spring WebFlux). Routes completion, embedding, and career prediction requests to external LLM providers or the Python sidecar. Enforces per-requester, per-model rate limiting via Redis. Publishes AI usage audit events to Kafka.

**Inbound:**
- `api-gateway` (proxies `/api/v1/ai/**`)
- Direct calls from `center-svc`, `parent-svc`, `assess-svc`, `psych-svc` via `${AI_GATEWAY_BASE_URL}`

**Outbound HTTP (all reactive `Mono<T>`):**

| Target | Adapter Class | Endpoint | Auth | Circuit Breaker |
|--------|--------------|---------|------|-----------------|
| Anthropic Claude API | `AnthropicWebClientAdapter` | `POST ${ANTHROPIC_BASE_URL}/v1/messages` | `x-api-key: ${ANTHROPIC_API_KEY}`, `anthropic-version: 2023-06-01` | `anthropic` CB + `R4J_TL_AI_TIMEOUT` time limiter |
| OpenAI API | `OpenAiEmbeddingAdapter` | `POST https://api.openai.com/v1/embeddings` | `Authorization: Bearer ${OPENAI_API_KEY}` | None (Phase 2) |
| Ollama | (future adapter) | `${OLLAMA_BASE_URL}` | None | Phase 2 placeholder |
| `psych-ai-svc` sidecar | `PsychAiSidecarWebClientAdapter` | `POST ${PSYCH_AI_SVC_BASE_URL}/api/v1/predict-careers` | None (internal network trust) | `psych-ai-svc` CB |

The `AnthropicWebClientAdapter` raises `AiProviderException` for `LlmProvider.OPENAI` and `LlmProvider.OLLAMA` — those providers require their own adapter implementations.

**Rate limiting:**
- `RateLimitPort` → `RedisRateLimitAdapter` (`ReactiveRedisTemplate`).
- Per key: `{requesterId}:{modelType}` — Redis `INCR` + `EXPIRE` per sliding window.
- On Redis outage: `onErrorReturn(true)` — rate limiting **fails open** (requests allowed through). By design.

**Outbound async (Kafka topics published):**

| Topic (env var) | Events | Key | Trigger |
|-----------------|--------|-----|---------|
| `${KAFKA_TOPIC_AI_GATEWAY_EVENTS}` | `AiRequestRoutedEvent` (success), `AiRequestFailedEvent` (failure) | `requestId` (UUID string) | On each AI request completion or failure |
| `${KAFKA_TOPIC_AUDIT_IMMUTABLE}` | Same events duplicated to audit | `requestId` | On each AI request |

Adapter: `AiGatewayKafkaAdapter` (implements `AiGatewayEventPublisher`). Void (not reactive) — fire-and-forget. Exceptions caught in `sendBestEffort()` and logged, never propagated.

**Kafka consumed:** None in Phase 1.

**Database:** None. Fully stateless — no JPA, no datasource, no Flyway.

**Cache (Redis):**

| Usage | Port Interface | Adapter Class | Key Pattern |
|-------|---------------|---------------|-------------|
| Per-requester per-model rate counter | `RateLimitPort` | `RedisRateLimitAdapter` | `rate:{requesterId}:{modelType}` — INCR + EXPIRE |

**API endpoints:**

| Method | Path | Use Case Interface | Provider |
|--------|------|--------------------|---------|
| POST | `/api/v1/ai/completions` | `RouteCompletionUseCase` | Anthropic Claude (primary) |
| POST | `/api/v1/ai/embeddings` | `RouteEmbeddingUseCase` | OpenAI text-embedding |
| POST | `/api/v1/ai/career-predictions` | `RouteCareerPredictionUseCase` | psych-ai-svc sidecar |

**Outbound ports → adapters:**

| Domain Port Interface | Infrastructure Adapter |
|-----------------------|------------------------|
| `LlmClient` | `AnthropicWebClientAdapter` |
| `EmbeddingClient` | `OpenAiEmbeddingAdapter` |
| `PsychAiSidecarClient` | `PsychAiSidecarWebClientAdapter` |
| `RateLimitPort` | `RedisRateLimitAdapter` |
| `AiGatewayEventPublisher` | `AiGatewayKafkaAdapter` |

---

## 3. Startup Order

### 3.1 Infrastructure Services (must reach healthy state before any application service starts)

```
Step 1 — PostgreSQL 16 (timescale/timescaledb-ha:pg16-latest)
   Health check: pg_isready -U ${POSTGRES_ROOT_USER}
   Interval: 10s | Timeout: 5s | Retries: 5 | Start period: 20s
   Extensions provisioned at first start: uuid-ossp, pgcrypto, pgvector (via init scripts)

Step 2 — Redis 7 (redis:7-alpine)
   Health check: redis-cli -a ${REDIS_PASSWORD} ping
   Interval: 10s | Timeout: 3s | Retries: 5
   Config: appendonly yes, appendfsync everysec

Step 3 — Kafka 3.6 (bitnami/kafka:3.6, KRaft mode — no ZooKeeper)
   Health check: kafka-topics.sh --bootstrap-server localhost:9092 --list
   Interval: 30s | Timeout: 10s | Retries: 5 | Start period: 30s
```

### 3.2 Application Services — Recommended Order

```
Step 4 — auth-svc
   Hard requires: PostgreSQL (auth_schema Flyway migrations), Redis, Kafka
   Flyway: classpath:db/migration/auth — creates auth_schema tables
   No dependency on any other application service
   Must be healthy before client sessions can be established, but other
   services do not call auth-svc at runtime (JWT validated locally)

Step 5 — center-svc
   Hard requires: PostgreSQL (center_schema Flyway migrations), Kafka
   Soft requires: ai-gateway-svc (circuit breaker absorbs absence at startup)
   Flyway: classpath:db/migration/center

Step 6 — ai-gateway-svc
   Hard requires: Redis (rate limiting), Kafka
   Soft requires: Anthropic API key valid, OpenAI API key valid, psych-ai-svc reachable
   No PostgreSQL dependency — stateless
   Should be healthy before services that call it (center-svc, parent-svc,
   assess-svc, psych-svc) handle real AI-dependent requests

Step 7 — parent-svc
   Hard requires: PostgreSQL (parent_schema Flyway), Kafka, Firebase credentials
   Soft requires: ai-gateway-svc (circuit breaker)
   Flyway: classpath:db/migration/parent
   Consumes center-events (Kafka must be healthy; auto-offset-reset: earliest)

Step 8 — assess-svc
   Hard requires: PostgreSQL (assess_schema Flyway), Kafka
   Soft requires: ai-gateway-svc (circuit breaker)
   Flyway: classpath:db/migration/assess (V1–V7)
   Consumes center-events

Step 9 — psych-ai-svc (Python FastAPI sidecar — not in this repo)
   Hard requires: ML model weights loaded
   psych-svc circuit breaker handles sidecar absence gracefully at runtime
   but sidecar should be started before psych-svc to avoid initial CB opens

Step 10 — psych-svc
   Hard requires: PostgreSQL (psych_schema Flyway), Kafka
   Soft requires: psych-ai-svc (circuit breaker absorbs absence; career
   mappings are saved as FAILED status instead of failing the request)
   Soft requires: ai-gateway-svc (circuit breaker)
   Flyway: classpath:db/migration/psych (V1–V5, V2 seeds trait_dimensions)
   Consumes assess-events

Step 11 — api-gateway
   Hard requires: Redis (rate limiting)
   Soft requires: all downstream services reachable (gateway returns 503
   per route for any unresponsive downstream)
   No PostgreSQL — stateless
   Start last — it is the public ingress point
```

### 3.3 Docker Compose (Local Dev)

```bash
# Start infrastructure and wait for health
docker compose --env-file ../../.env up -d postgres redis kafka

# Wait for health checks (postgres ~20s, kafka ~30s)
docker compose --env-file ../../.env up -d auth-svc center-svc ai-gateway-svc

# Wait for Flyway migrations to complete (check: docker compose logs auth-svc)
docker compose --env-file ../../.env up -d parent-svc assess-svc

# Start Python sidecar (separate process or container)
# docker run ... psych-ai-svc:latest

docker compose --env-file ../../.env up -d psych-svc

# Start edge gateway last
docker compose --env-file ../../.env up -d api-gateway
```

### 3.4 Kubernetes (Production)

- Each service pod has a `readinessProbe` on `GET /actuator/health` (HTTP 200 = ready).
- `startupProbe` with `failureThreshold: 30`, `periodSeconds: 10` provides a 5-minute window for Flyway migrations and cold DB connection establishment.
- PostgreSQL, Redis, and Kafka are deployed as separate StatefulSet/Helm-managed clusters before application services.
- `psych-ai-svc` is deployed as a sidecar container in the `psych-svc` Pod (shared network namespace, no service discovery needed) or as a separate Deployment with a ClusterIP Service.

---

## 4. Failure Impact Analysis

### 4.1 PostgreSQL Down

| Affected Service | User-Visible Impact | Internal Behaviour |
|-----------------|--------------------|--------------------|
| auth-svc | Cannot register or log in; cannot issue new tokens | `DataAccessException` propagates → `GlobalExceptionHandler` → HTTP 503 |
| center-svc | All center/batch/content operations fail | HTTP 503 |
| parent-svc | All parent profile operations fail | HTTP 503 |
| assess-svc | All exam/submission/grade operations fail | HTTP 503 |
| psych-svc | All profile/session/mapping operations fail | HTTP 503 |
| api-gateway | Unaffected — no database | Routing continues; downstream services return 503 |
| ai-gateway-svc | Unaffected — no database | AI routing continues |

**Note on existing JWT tokens:** Tokens already issued before the outage remain cryptographically valid until their TTL expires. Downstream services can still validate them using their locally loaded public key, so read-only operations that do not require DB access may partially succeed (unlikely in practice — all services are DB-bound for their primary operations).

**Recovery:** HikariCP reconnects automatically when PostgreSQL recovers. Flyway does not re-run migrations — they are tracked in `flyway_schema_history`.

---

### 4.2 Redis Down

| Service | Subsystem Affected | Impact | Designed Behaviour |
|---------|--------------------|--------|--------------------|
| api-gateway | Token-bucket rate limiting | Rate limiting **fails open** | Requests pass through unmetered. Documented design: no traffic loss over rate-limit enforcement |
| auth-svc | Refresh token store | `TokenStore.find()` fails → token refresh fails; `TokenStore.deleteAllForUser()` fails → logout fails | HTTP 500 on refresh/logout endpoints |
| auth-svc | OTP store | `OtpStore.save()` and `OtpStore.find()` fail → OTP flow fails entirely | HTTP 500 on OTP endpoints; registration, password reset, MFA login blocked |
| ai-gateway-svc | Per-requester rate limit counters | Rate limiting **fails open** (`onErrorReturn(true)` in reactive chain) | All AI requests allowed through regardless of quota |
| center-svc | None active Phase 1 | No impact | |
| parent-svc | None active Phase 1 | No impact | |
| assess-svc | None active Phase 1 | No impact | |
| psych-svc | None active Phase 1 | No impact | |

**Recovery:** Redis reconnects automatically. Rate-limit counters reset on restart (sliding window starts fresh — acceptable given eventual-consistency rate-limiting design). Refresh tokens stored in Redis are lost on Redis restart if AOF is not enabled or the node is cold-started; users will need to re-authenticate.

---

### 4.3 Kafka Down

| Impact Category | Detail |
|----------------|--------|
| Domain operations | All user-facing operations succeed. The `@Transactional` DB commit completes before Kafka publish is attempted. |
| Audit events | All audit events are silently dropped for the duration of the outage. Logged at `ERROR` level. No compensation mechanism in Phase 1 (outbox pattern is a Phase 2 upgrade path). |
| parent-svc `CenterEventConsumer` | Stops receiving center-events. Phase 1: no user impact. Phase 2: parent push notifications stop firing. |
| assess-svc `CenterEventConsumer` | Stops receiving center-events. Phase 1: no user impact. Phase 2: batch-close invalidation of submissions stops. |
| psych-svc `AssessEventConsumer` | Stops receiving assess-events. Phase 1: no user impact. Phase 2: grade-triggered sessions stop firing. |

**Recovery:** Kafka consumers resume from their last committed offset (`auto-offset-reset: earliest` for new groups ensures no messages from before first launch are skipped). Events published while Kafka was down are permanently lost — there is no at-least-once guarantee in Phase 1.

---

### 4.4 psych-ai-svc (Python Sidecar) Down

| Caller | Impact | Behaviour |
|--------|--------|-----------|
| psych-svc | Career mapping requests fail gracefully | `PsychAiSvcWebClientAdapter.predictCareers()` is annotated `@CircuitBreaker(name = "psych-ai-svc")`. When the CB opens, it throws `RuntimeException`. `CareerMappingService.requestCareerMapping()` catches it → calls `mapping.fail()` → saves `CareerMapping` with status `FAILED` → DB transaction commits → returns HTTP 201 with `FAILED` status in response body. No HTTP 5xx to the client. |
| ai-gateway-svc | Career prediction routing fails | `PsychAiSidecarWebClientAdapter.predictCareers()` → CB opens → `AiProviderException` → HTTP 502 returned to caller. |

**Recovery:** Both circuit breakers transition to HALF_OPEN after `wait-duration-in-open-state`. First successful probe closes the circuit. `FAILED` career mappings must be explicitly re-requested by the student — there is no automatic retry or background job in Phase 1.

---

### 4.5 Anthropic API Down

| Affected Path | Impact | Behaviour |
|--------------|--------|-----------|
| ai-gateway-svc `/api/v1/ai/completions` | LLM completions fail | `AnthropicWebClientAdapter` receives error → `AiProviderException("Anthropic API error [status]: body")` → circuit breaker `anthropic` increments failure count → on threshold: CB opens → all subsequent completion requests fail fast with `CallNotPermittedException` → `AiProviderException` → HTTP 502. |
| center-svc, parent-svc, assess-svc calling ai-gateway-svc | AI-dependent features degrade | Each service has its own `ai-gateway` circuit breaker. On enough 502s from ai-gateway-svc, each service's CB opens, failing fast. Core non-AI operations (CRUD) continue to succeed. |
| Ollama fallback | Not automatic in Phase 1 | `LlmProvider.OLLAMA` raises `AiProviderException("Ollama routing not implemented")` — there is no automatic fallback from Anthropic to Ollama. Phase 2 upgrade: implement `OllamaWebClientAdapter` and add provider selection logic. |

---

### 4.6 OpenAI API Down

| Affected Path | Impact | Behaviour |
|--------------|--------|-----------|
| ai-gateway-svc `/api/v1/ai/embeddings` | Embedding requests fail | `OpenAiEmbeddingAdapter` returns error → `AiProviderException` → HTTP 502. No circuit breaker on OpenAI in Phase 1. |
| assess-svc question similarity search | Embedding indexing fails | New question embeddings cannot be generated; similarity search returns degraded results. Existing embeddings are unaffected. |
| psych-svc embedding enrichment | Embedding field remains null | `embedding_json` column stays null until OpenAI recovers. Phase 2: pgvector HNSW index will be affected. |

---

### 4.7 hCaptcha API Down

| Affected Endpoints | Impact | Behaviour |
|-------------------|--------|-----------|
| auth-svc `/api/v1/auth/register`, `/api/v1/auth/login` | New registrations and captcha-verified logins blocked | `HCaptchaRestAdapter` circuit breaker `captcha-client` opens → fallback method `captchaFallback()` returns `false` → `CaptchaVerifier.verify()` returns `false` → auth service rejects the request with HTTP 400 "Invalid captcha." |

**Design rationale for fail-closed:** The hCaptcha circuit breaker deliberately fails closed (rejects requests) rather than open (allows). Allowing unverified registrations during an outage would defeat bot protection, which is a security requirement.

---

### 4.8 SMTP Server Down

| Affected Flows | Impact | Behaviour |
|---------------|--------|-----------|
| auth-svc OTP email delivery | Email verification, password reset, MFA login via email OTP blocked | `SmtpNotificationAdapter.sendOtpEmail()` throws `MessagingException` → wrapped as `IllegalStateException("Email delivery failed")` → propagates up the call stack → HTTP 500. No circuit breaker on SMTP — the exception propagates directly. |

SMS OTP path via Twilio is unaffected by SMTP outage.

---

### 4.9 api-gateway Down

All external client traffic is blocked. Downstream services continue operating normally — they are not aware the gateway is down. Internal service-to-service calls (psych-svc → psych-ai-svc, any service → ai-gateway-svc) are unaffected. Re-deploy the gateway pod to restore external access; downstream service state is unaffected.

---

## 5. External Dependencies

### 5.1 Anthropic API (Claude LLM)

| Attribute | Detail |
|-----------|--------|
| Purpose | Primary LLM for text completion requests |
| Caller | `ai-gateway-svc` — `AnthropicWebClientAdapter` |
| Base URL | `${ANTHROPIC_BASE_URL}` (e.g. `https://api.anthropic.com`) |
| Endpoint | `POST /v1/messages` |
| Auth headers | `x-api-key: ${ANTHROPIC_API_KEY}`, `anthropic-version: 2023-06-01` |
| Model | `${ANTHROPIC_MODEL}` (e.g. `claude-sonnet-4-6`) |
| Request format | `{ model, max_tokens, messages: [{role: "user", content: "[System]: {system}\n\n{user}"}] }` |
| Response parsing | `content[0].text` for completion text; `usage.input_tokens` + `usage.output_tokens` for cost tracking |
| Circuit breaker | `anthropic` (Resilience4j) + `anthropic` time limiter (`${R4J_TL_AI_TIMEOUT}`) |
| Error mapping | Non-2xx → `AiProviderException("Anthropic API error [status]: body")` → HTTP 502 |
| Token tracking | Latency (ms), input tokens, output tokens included in `CompletionResponse` record and `AiRequestRoutedEvent` |

---

### 5.2 OpenAI API (Embeddings)

| Attribute | Detail |
|-----------|--------|
| Purpose | Vector embedding generation for question similarity search and content retrieval |
| Caller | `ai-gateway-svc` — `OpenAiEmbeddingAdapter` |
| Base URL | `https://api.openai.com` (hardcoded) |
| Endpoint | `POST /v1/embeddings` |
| Auth header | `Authorization: Bearer ${OPENAI_API_KEY}` |
| Model | `${AI_EMBEDDING_MODEL}` (e.g. `text-embedding-3-small`) |
| Dimensions | `${AI_EMBEDDING_DIMENSIONS}` (e.g. 1536) — must match the `vector(N)` column in assess_schema and psych_schema |
| Request format | `{ model, input: "text string" }` |
| Response parsing | `data[0].embedding` — `List<Double>` of dimension N |
| Error mapping | Non-2xx → `AiProviderException("OpenAI Embeddings API error [status]: body")` → HTTP 502 |
| Latency tracking | `latencyMs` calculated from `startMs` to response, included in `EmbeddingResponse` |

---

### 5.3 Ollama (Self-Hosted LLM)

| Attribute | Detail |
|-----------|--------|
| Purpose | Local LLM for development environments and offline fallback |
| Caller | `ai-gateway-svc` (adapter not yet implemented in Phase 1) |
| Base URL | `${OLLAMA_BASE_URL}` |
| Auth | None (local service) |
| Phase 1 status | Placeholder — `LlmProvider.OLLAMA` in `AnthropicWebClientAdapter.complete()` raises `AiProviderException("Ollama routing not implemented in this adapter")`. No automatic failover from Anthropic. |
| Phase 2 plan | Implement `OllamaWebClientAdapter implements LlmClient` + routing logic in `LlmRoutingService` to select Ollama when `${LLM_PROVIDER}` env var = `OLLAMA` or as a fallback on Anthropic CB open. |

---

### 5.4 psych-ai-svc (Python FastAPI Sidecar)

| Attribute | Detail |
|-----------|--------|
| Purpose | Career prediction ML model — Big Five personality scores + RIASEC code → ranked career suggestions with AI reasoning |
| Technology | Python FastAPI (not in this repository) |
| Default port | 8095 |
| Deployment | Sidecar container alongside `psych-svc` Pod in Kubernetes, or standalone container in docker-compose |
| Callers | `psych-svc` (`PsychAiSvcWebClientAdapter`) and `ai-gateway-svc` (`PsychAiSidecarWebClientAdapter`) |
| Endpoint | `POST /api/v1/predict-careers` |
| Auth | None (internal network trust; mTLS is a Phase 2 hardening step) |
| Connect timeout | `${PSYCH_AI_SVC_CONNECT_TIMEOUT_SECONDS}` (both callers) |
| Read timeout | `${PSYCH_AI_SVC_READ_TIMEOUT_SECONDS}` (both callers) |
| Circuit breaker | `psych-ai-svc` on both `psych-svc` and `ai-gateway-svc` |

**Request payload:**
```json
{
  "profileId": "550e8400-e29b-41d4-a716-446655440000",
  "openness": 0.75,
  "conscientiousness": 0.82,
  "extraversion": 0.60,
  "agreeableness": 0.70,
  "neuroticism": 0.35,
  "riasecCode": "RIA"
}
```

**Response payload:**
```json
{
  "topCareers": ["Software Engineer", "Data Scientist", "Research Scientist"],
  "reasoning": "High openness and conscientiousness combined with Investigative RIASEC code...",
  "modelVersion": "psych-ai-v2"
}
```

**Failure modes:**
- `psych-svc` caller: circuit breaker opens → `CareerMappingService` catches → `CareerMapping.fail()` → status `FAILED` saved → HTTP 201 returned with FAILED body.
- `ai-gateway-svc` caller: circuit breaker opens → `AiProviderException` → HTTP 502 returned to caller.

---

### 5.5 hCaptcha Enterprise API

| Attribute | Detail |
|-----------|--------|
| Purpose | Bot protection on user registration and login endpoints |
| Caller | `auth-svc` — `HCaptchaRestAdapter` (Spring `RestClient`, blocking) |
| Endpoint | `POST ${HCAPTCHA_VERIFY_URL}` (e.g. `https://hcaptcha.com/siteverify`) |
| Request format | `application/x-www-form-urlencoded`: `secret=${HCAPTCHA_SECRET_KEY}`, `response={captchaToken}`, `remoteip={clientIp}` |
| Auth | Secret key embedded in request body |
| Site key | `${HCAPTCHA_SITE_KEY}` — distributed to front-end clients for widget rendering |
| Circuit breaker | `captcha-client` — fallback `captchaFallback()` returns `false` (fail **closed**: reject all requests during outage) |
| Success condition | `HCaptchaResponse.success() == true` |

---

### 5.6 SMTP Server (Email OTP Delivery)

| Attribute | Detail |
|-----------|--------|
| Purpose | OTP delivery for email verification, password reset, and MFA login |
| Caller | `auth-svc` — `SmtpNotificationAdapter` (JavaMailSender, blocking) |
| Config | `${MAIL_HOST}`, `${MAIL_PORT}`, `${MAIL_USERNAME}`, `${MAIL_PASSWORD}` |
| Protocol | SMTP with STARTTLS (`mail.smtp.auth: true`, `mail.smtp.starttls.enable: true`) |
| From identity | `${MAIL_FROM_ADDRESS}`, `${MAIL_FROM_NAME}` |
| OTP purposes mapped | `EMAIL_VERIFICATION` → "Verify your EduTech account"; `PASSWORD_RESET` → "Reset your EduTech password"; `LOGIN_MFA` → "Your EduTech login code" |
| Failure mode | `MessagingException` → `IllegalStateException("Email delivery failed")` → HTTP 500 (no circuit breaker) |

---

### 5.7 Twilio Verify API (SMS OTP)

| Attribute | Detail |
|-----------|--------|
| Purpose | SMS OTP delivery for MFA login and phone number verification |
| Caller | `auth-svc` — `SmtpNotificationAdapter.sendOtpSms()` |
| Config | `${TWILIO_ACCOUNT_SID}`, `${TWILIO_AUTH_TOKEN}`, `${TWILIO_VERIFY_SERVICE_SID}` |
| Production behaviour | Twilio SDK sends SMS via Verify API |
| Development behaviour | OTP logged to console: `log.info("[DEV-ONLY] SMS OTP for to={} purpose={} otp={}", ...)` — no actual SMS sent |

---

### 5.8 Firebase Cloud Messaging (Push Notifications)

| Attribute | Detail |
|-----------|--------|
| Purpose | Mobile push notifications to the parent mobile app (Android / iOS) |
| Caller | `parent-svc` exclusively (Firebase Admin SDK) |
| Config | `${FIREBASE_PROJECT_ID}`, `${FIREBASE_SERVICE_ACCOUNT_KEY_PATH}` |
| Usage | Parent Copilot notifications, batch status change alerts (Phase 2 integration with `CenterEventConsumer`) |
| Note | Only `parent-svc` has Firebase credentials configured; no other service uses Firebase |

---

*See also: `/docs/ARCHITECTURE.md` for the full system architecture document and `docs/services/*/COMPLETION.md` for per-service frozen implementation records.*
