# career-oracle-svc — Service Completion Document

```
DOCUMENT STATUS : FROZEN
SERVICE VERSION  : 1.0.0-SNAPSHOT
FROZEN ON        : 2026-03-08
BUILD STATUS     : PASSING — 15/15 tests green (17 with new ArchUnit rules)
JAVA             : 17 (local) / 21 (production container)
SPRING BOOT      : 3.3.5
```

> This document is a permanent, immutable record of career-oracle-svc as it stood at
> completion. It is never edited retroactively. Future changes to the service
> produce a new completion document at the next milestone.

---

## 1. Service Purpose

`career-oracle-svc` is the career intelligence boundary of the EduPath cluster.
It aggregates psychometric data (from `psych-svc`), academic performance data
(from `performance-svc`), and exam target data (from `student-profile-svc`) to
produce scored career recommendations using a deterministic `CareerScoreCalculator`
domain service, optionally enriched by AI via `ai-gateway-svc`.

**Responsibilities:**

| Responsibility | Mechanism |
|---|---|
| Career recommendation generation | CareerScoreCalculator domain service |
| Career profile persistence | JPA aggregate with 5 recommendation entries |
| Score calculation | Weighted scoring across personality, aptitude, interests |
| Domain event publication | Kafka: career-recommended, career-profile-created |

---

## 2. Architectural Philosophy

Hexagonal architecture (Ports & Adapters):
- **Domain layer** — `CareerRecommendation`, `CareerProfile` aggregates + `CareerScoreCalculator` domain service
- **Application layer** — `CareerRecommendationService` + use-case interfaces
- **Infrastructure layer** — JPA adapters, Kafka adapter, Security
- **API layer** — REST controllers + GlobalExceptionHandler (RFC 7807)

Security model: JWT validated upstream by `student-gateway`. This service reads
`X-User-Id` / `X-User-Role` headers via `GatewayHeaderAuthFilter`.
Requests without these headers are rejected with HTTP 403.

---

## 3. Package Structure

```
com.edutech.careeroracle
├── CareerOracleApplication.java
├── api/
│   ├── CareerRecommendationController.java
│   └── GlobalExceptionHandler.java
├── application/
│   ├── dto/          (Java Records)
│   ├── exception/    (CareerOracleException, RecommendationNotFoundException)
│   └── service/      (CareerRecommendationService)
├── domain/
│   ├── event/        (CareerRecommendedEvent, CareerProfileCreatedEvent)
│   ├── model/        (CareerRecommendation, CareerProfile, CareerDomain enum)
│   ├── port/
│   │   ├── in/       (use-case interfaces)
│   │   └── out/      (repository + publisher interfaces)
│   └── service/      (CareerScoreCalculator — pure domain logic)
└── infrastructure/
    ├── config/       (KafkaTopicProperties)
    ├── kafka/        (CareerOracleKafkaAdapter)
    ├── persistence/  (Spring Data repos + adapter implementations)
    └── security/     (GatewayHeaderAuthFilter, SecurityConfig)
```

**Production files:** 45 | **Test files:** 3

---

## 4. Domain Model

| Entity / Service | Key Behaviour |
|-----------------|---------------|
| `CareerProfile` | Aggregate; holds ordered `CareerRecommendation` list |
| `CareerRecommendation` | Score (0–100), rank, career domain label |
| `CareerScoreCalculator` | Pure domain service; weighted formula across Big Five + RIASEC + grades |

---

## 5. API Contract

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/v1/career-profiles` | STUDENT | Generate/refresh career recommendations |
| `GET` | `/api/v1/career-profiles/{studentId}` | STUDENT | Fetch career profile |
| `GET` | `/api/v1/career-profiles/{studentId}/top` | STUDENT | Top N recommendations |

All error responses use RFC 7807 `ProblemDetail`.

---

## 6. Kafka Events

### Published

| Topic (env var) | Event Record | Trigger |
|-----------------|-------------|---------|
| `KAFKA_TOPIC_CAREER_ORACLE_RECOMMENDED` | `CareerRecommendedEvent` | Profile generated |
| `KAFKA_TOPIC_CAREER_ORACLE_PROFILE_CREATED` | `CareerProfileCreatedEvent` | First profile creation |

---

## 7. Configuration Reference

| Environment Variable | Required | Description |
|---------------------|----------|-------------|
| `CAREER_ORACLE_SVC_PORT` | No (default: 8087) | HTTP port |
| `POSTGRES_HOST` | **Yes** | PostgreSQL host |
| `POSTGRES_PORT` | **Yes** | PostgreSQL port |
| `CAREER_ORACLE_DB_NAME` | **Yes** | Database name |
| `CAREER_ORACLE_DB_USER` | **Yes** | Database username |
| `CAREER_ORACLE_DB_PASSWORD` | **Yes** | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | **Yes** | Kafka bootstrap |
| `KAFKA_TOPIC_CAREER_ORACLE_RECOMMENDED` | **Yes** | Topic name |
| `KAFKA_TOPIC_CAREER_ORACLE_PROFILE_CREATED` | **Yes** | Topic name |

Database schema: `careeroracle_schema`. Flyway migrations at `classpath:db/migration`.

---

## 8. Test Coverage

| Test Class | Tests | Scope |
|------------|-------|-------|
| `CareerRecommendationServiceTest` | 5 | Unit — recommendation CRUD + scoring |
| `CareerScoreCalculatorTest` | 5 | Unit — pure domain scoring algorithm |
| `ArchitectureRulesTest` | 7 | ArchUnit — hexagonal rules + no-Lombok |

**Total: 17/17 passing**
