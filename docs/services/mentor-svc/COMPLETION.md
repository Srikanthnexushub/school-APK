# mentor-svc — Service Completion Document

```
DOCUMENT STATUS : FROZEN
SERVICE VERSION  : 1.0.0-SNAPSHOT
FROZEN ON        : 2026-03-08
BUILD STATUS     : PASSING — 15/15 tests green (17 with new ArchUnit rules)
JAVA             : 17 (local) / 21 (production container)
SPRING BOOT      : 3.3.5
```

> This document is a permanent, immutable record of mentor-svc as it stood at
> completion. It is never edited retroactively. Future changes to the service
> produce a new completion document at the next milestone.

---

## 1. Service Purpose

`mentor-svc` is the human mentoring boundary of the EduPath cluster.
It manages mentor profiles (expert educators), session bookings between students
and mentors, session lifecycle (booked → in-progress → completed), and feedback
submission. It is strictly separate from `ai-mentor-svc` which handles AI-driven
mentoring.

**Responsibilities:**

| Responsibility | Mechanism |
|---|---|
| Mentor profile management | CRUD for mentor expertise, bio, availability |
| Session booking | Student books slot with available mentor |
| Session lifecycle | BOOKED → IN_PROGRESS → COMPLETED state machine |
| Feedback collection | Post-session student feedback stored + published |
| Domain event publication | Kafka: session-booked, session-completed, feedback-submitted |

---

## 2. Architectural Philosophy

Hexagonal architecture (Ports & Adapters):
- **Domain layer** — `MentorProfile`, `MentorSession` aggregates with rich state machine behaviour
- **Application layer** — `MentorProfileService`, `MentorSessionService` + use-case interfaces
- **Infrastructure layer** — JPA adapters, Kafka adapter, Security
- **API layer** — REST controllers + GlobalExceptionHandler (RFC 7807)

Security model: JWT validated upstream by `student-gateway`. This service reads
`X-User-Id` / `X-User-Role` headers via `GatewayHeaderAuthFilter`.
Requests without these headers are rejected with HTTP 403.

---

## 3. Package Structure

```
com.edutech.mentorsvc
├── MentorSvcApplication.java
├── api/
│   ├── MentorProfileController.java
│   ├── MentorSessionController.java
│   └── GlobalExceptionHandler.java
├── application/
│   ├── dto/          (Java Records)
│   ├── exception/    (MentorSvcException, ProfileNotFoundException, SessionNotFoundException)
│   └── service/      (MentorProfileService, MentorSessionService)
├── domain/
│   ├── event/        (SessionBookedEvent, SessionCompletedEvent, FeedbackSubmittedEvent)
│   ├── model/        (MentorProfile, MentorSession, SessionStatus, ExpertiseArea)
│   └── port/
│       ├── in/       (use-case interfaces)
│       └── out/      (repository + publisher interfaces)
└── infrastructure/
    ├── config/       (KafkaTopicProperties)
    ├── kafka/        (MentorKafkaAdapter)
    ├── persistence/  (Spring Data repos + adapter implementations)
    └── security/     (GatewayHeaderAuthFilter, SecurityConfig)
```

**Production files:** 49 | **Test files:** 3

---

## 4. Domain Model

| Entity | Key Behaviour |
|--------|---------------|
| `MentorProfile` | Aggregate; `updateAvailability()`, `addExpertise()`, `deactivate()` |
| `MentorSession` | State machine: `book()` → `start()` → `complete(feedback)` |
| `SessionStatus` | BOOKED / IN_PROGRESS / COMPLETED / CANCELLED |
| `ExpertiseArea` | Enum: MATHEMATICS, PHYSICS, CHEMISTRY, BIOLOGY, ENGLISH, etc. |

---

## 5. API Contract

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/mentors` | ADMIN | Register mentor profile |
| `GET` | `/api/v1/mentors/{id}` | STUDENT | Get mentor profile |
| `GET` | `/api/v1/mentors` | STUDENT | List available mentors |
| `PUT` | `/api/v1/mentors/{id}` | ADMIN | Update mentor profile |
| `POST` | `/api/v1/sessions` | STUDENT | Book mentor session |
| `GET` | `/api/v1/sessions/{id}` | STUDENT | Get session details |
| `PATCH` | `/api/v1/sessions/{id}/start` | MENTOR | Mark session as started |
| `PATCH` | `/api/v1/sessions/{id}/complete` | MENTOR | Complete session + submit feedback |

All error responses use RFC 7807 `ProblemDetail`.

---

## 6. Kafka Events

### Published

| Topic (env var) | Event Record | Trigger |
|-----------------|-------------|---------|
| `KAFKA_TOPIC_MENTOR_SESSION_BOOKED` | `SessionBookedEvent` | Session booked |
| `KAFKA_TOPIC_MENTOR_SESSION_COMPLETED` | `SessionCompletedEvent` | Session completed |
| `KAFKA_TOPIC_MENTOR_SESSION_FEEDBACK` | `FeedbackSubmittedEvent` | Feedback submitted |

---

## 7. Configuration Reference

| Environment Variable | Required | Description |
|---------------------|----------|-------------|
| `MENTOR_SVC_PORT` | No (default: 8088) | HTTP port |
| `POSTGRES_HOST` | **Yes** | PostgreSQL host |
| `POSTGRES_PORT` | **Yes** | PostgreSQL port |
| `MENTOR_DB_NAME` | **Yes** | Database name |
| `MENTOR_DB_USER` | **Yes** | Database username |
| `MENTOR_DB_PASSWORD` | **Yes** | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | **Yes** | Kafka bootstrap |
| `KAFKA_TOPIC_MENTOR_SESSION_BOOKED` | **Yes** | Topic name |
| `KAFKA_TOPIC_MENTOR_SESSION_COMPLETED` | **Yes** | Topic name |
| `KAFKA_TOPIC_MENTOR_SESSION_FEEDBACK` | **Yes** | Topic name |

Database schema: `mentor_schema`. Flyway migrations at `classpath:db/migration`.

---

## 8. Test Coverage

| Test Class | Tests | Scope |
|------------|-------|-------|
| `MentorProfileServiceTest` | 5 | Unit — profile CRUD + availability |
| `MentorSessionServiceTest` | 5 | Unit — session state machine |
| `ArchitectureRulesTest` | 7 | ArchUnit — hexagonal rules + no-Lombok |

**Total: 17/17 passing**
