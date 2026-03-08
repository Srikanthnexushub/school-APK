# ai-mentor-svc — Service Completion Document

```
DOCUMENT STATUS : FROZEN
SERVICE VERSION  : 1.0.0-SNAPSHOT
FROZEN ON        : 2026-03-08
BUILD STATUS     : PASSING — 15/15 tests green
JAVA             : 17 (local) / 21 (production container)
SPRING BOOT      : 3.3.5
```

> This document is a permanent, immutable record of ai-mentor-svc as it stood at
> completion. It is never edited retroactively. Future changes to the service
> produce a new completion document at the next milestone.

---

## 1. Service Purpose

`ai-mentor-svc` is the AI-powered academic mentoring boundary of the EduPath cluster.
It provides personalised study plans, doubt resolution, and recommendation features
by orchestrating calls to `ai-gateway-svc` (LLM routing) while maintaining its own
persistent state for plans, doubts, and recommendations.

**Responsibilities:**

| Responsibility | Mechanism |
|---|---|
| Study plan generation | AI-driven plan creation via ai-gateway-svc WebClient |
| Doubt ticket lifecycle | Submit → Pending → Resolved via AI or manual |
| Personalised recommendations | Stored in DB, refreshed by Kafka events from performance-svc |
| Domain event publication | Kafka: study-plan-created, doubt-submitted, doubt-resolved |

---

## 2. Architectural Philosophy

Hexagonal architecture (Ports & Adapters):
- **Domain layer** — `DoubtTicket`, `StudyPlan`, `StudyPlanItem`, `Recommendation` (JPA entities with rich behaviour)
- **Application layer** — `StudyPlanService`, `DoubtService`, `RecommendationService` + use-case interfaces
- **Infrastructure layer** — JPA adapters, Kafka adapter, WebClient HTTP adapter, Security
- **API layer** — REST controllers + GlobalExceptionHandler (RFC 7807)

Security model: JWT validated upstream by `student-gateway`. This service reads
`X-User-Id` / `X-User-Role` headers injected by the gateway via `GatewayHeaderAuthFilter`.
Requests lacking these headers are rejected with HTTP 403 (`anyRequest().authenticated()`).

---

## 3. Package Structure

```
com.edutech.aimentor
├── AiMentorApplication.java
├── api/
│   ├── DoubtController.java
│   ├── RecommendationController.java
│   ├── StudyPlanController.java
│   └── GlobalExceptionHandler.java
├── application/
│   ├── dto/          (6 Java Records)
│   ├── exception/    (AiMentorException, DoubtNotFoundException, StudyPlanNotFoundException)
│   └── service/      (StudyPlanService, DoubtService, RecommendationService)
├── domain/
│   ├── event/        (StudyPlanCreatedEvent, DoubtSubmittedEvent, DoubtResolvedEvent)
│   ├── model/        (DoubtTicket, StudyPlan, StudyPlanItem, Recommendation, enums)
│   └── port/
│       ├── in/       (6 use-case interfaces)
│       └── out/      (5 repository + publisher interfaces)
└── infrastructure/
    ├── config/       (KafkaTopicProperties, WebClientConfig)
    ├── http/         (AiGatewayHttpClient)
    ├── kafka/        (AiMentorKafkaAdapter, PerformanceEventConsumer)
    ├── persistence/  (5 Spring Data repos + 3 adapter implementations)
    └── security/     (GatewayHeaderAuthFilter, SecurityConfig)
```

**Production files:** 52 | **Test files:** 3

---

## 4. Domain Model

| Entity | Key Behaviour |
|--------|---------------|
| `StudyPlan` | Aggregate with `StudyPlanItem` list; `addItem()`, `markItemComplete()` |
| `StudyPlanItem` | `PriorityLevel` (HIGH/MEDIUM/LOW), `SubjectArea`, completion flag |
| `DoubtTicket` | State machine: PENDING → RESOLVED; `resolve(answer)` domain method |
| `Recommendation` | Lightweight entity; refreshed from external events |

---

## 5. API Contract

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/study-plans` | STUDENT | Create study plan via AI |
| `GET` | `/api/v1/study-plans/{id}` | STUDENT | Fetch study plan |
| `PATCH` | `/api/v1/study-plans/{planId}/items/{itemId}` | STUDENT | Mark item complete |
| `POST` | `/api/v1/doubts` | STUDENT | Submit doubt ticket |
| `GET` | `/api/v1/doubts/{id}` | STUDENT | Fetch doubt ticket |
| `GET` | `/api/v1/recommendations` | STUDENT | List personalised recommendations |

All error responses use RFC 7807 `ProblemDetail`.

---

## 6. Kafka Events

### Published

| Topic (env var) | Event Record | Trigger |
|-----------------|-------------|---------|
| `KAFKA_TOPIC_AI_MENTOR_STUDY_PLAN_CREATED` | `StudyPlanCreatedEvent` | Study plan created |
| `KAFKA_TOPIC_AI_MENTOR_DOUBT_SUBMITTED` | `DoubtSubmittedEvent` | Doubt ticket submitted |
| `KAFKA_TOPIC_AI_MENTOR_DOUBT_RESOLVED` | `DoubtResolvedEvent` | Doubt resolved |

### Consumed

| Source | Purpose |
|--------|---------|
| `performance-svc` events | Refresh recommendations when performance changes |

---

## 7. Infrastructure Adapters

| Adapter | Interface | Implementation |
|---------|-----------|----------------|
| `StudyPlanPersistenceAdapter` | `StudyPlanRepository` | Spring Data JPA |
| `DoubtTicketPersistenceAdapter` | `DoubtTicketRepository` | Spring Data JPA |
| `RecommendationPersistenceAdapter` | `RecommendationRepository` | Spring Data JPA |
| `AiGatewayHttpClient` | `AiGatewayClient` | WebClient (reactive) |
| `AiMentorKafkaAdapter` | `AiMentorEventPublisher` | Spring Kafka |

---

## 8. Configuration Reference

| Environment Variable | Required | Description |
|---------------------|----------|-------------|
| `AI_MENTOR_SVC_PORT` | No (default: 8086) | HTTP port |
| `POSTGRES_HOST` | **Yes** | PostgreSQL host |
| `POSTGRES_PORT` | **Yes** | PostgreSQL port |
| `AI_MENTOR_DB_NAME` | **Yes** | Database name |
| `AI_MENTOR_DB_USER` | **Yes** | Database username |
| `AI_MENTOR_DB_PASSWORD` | **Yes** | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | **Yes** | Kafka bootstrap |
| `KAFKA_TOPIC_AI_MENTOR_STUDY_PLAN_CREATED` | **Yes** | Topic name |
| `KAFKA_TOPIC_AI_MENTOR_DOUBT_SUBMITTED` | **Yes** | Topic name |
| `KAFKA_TOPIC_AI_MENTOR_DOUBT_RESOLVED` | **Yes** | Topic name |
| `AI_GATEWAY_SVC_URI` | **Yes** | ai-gateway-svc base URI |

Database schema: `aimentor_schema`. Flyway migrations at `classpath:db/migration`.

---

## 9. Observability

- Actuator: `health`, `info`, `metrics`
- Structured logging via SLF4J (MDC not yet populated with trace IDs — future work)
- OpenTelemetry configured via `OTEL_EXPORTER_OTLP_ENDPOINT`

---

## 10. Test Coverage

| Test Class | Tests | Scope |
|------------|-------|-------|
| `StudyPlanServiceTest` | 5 | Unit — study plan CRUD + AI delegation |
| `DoubtServiceTest` | 5 | Unit — doubt lifecycle state machine |
| `ArchitectureRulesTest` | 5 | ArchUnit — hexagonal layer isolation + no-Lombok |

**Total: 15/15 passing**

---

## 11. Known Constraints and Upgrade Paths

- JPA annotations reside in the domain layer (pragmatic compromise; see ADR in ArchUnit test)
- AI response quality depends on `ai-gateway-svc` LLM routing — no local fallback
- Study plan generation is synchronous (WebClient with timeout); future work: async via Kafka
