# student-gateway — COMPLETION RECORD

> **STATUS: FROZEN — DO NOT MODIFY**
> Tests passed: 10/10 | Build: SUCCESS | Date: 2026-03-07

---

## Service Overview

EduPath Student-Facing Edge Gateway — Spring Cloud Gateway that validates RS256 JWT tokens (issued by auth-svc) and routes requests to the 6 EduPath student services. Injects `X-User-Id` and `X-User-Role` headers so downstream services can trust them without re-validating JWT.

---

## Test Results

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS — Total time: 5.620 s
```

| Test Class | Tests | Result |
|---|---|---|
| ArchitectureRulesTest | 5 | PASS |
| JwtTokenValidatorTest | 5 | PASS |

---

## Architecture

Pure Spring Cloud Gateway (WebFlux/reactive). No database, no JPA, no Flyway.

```
com.edutech.studentgateway/
  StudentGatewayApplication.java
  config/
    JwtProperties.java         record, @ConfigurationProperties("jwt")
    SecurityConfig.java        WebFlux security — all auth delegated to JwtAuthenticationFilter
  filter/
    RequestIdFilter.java       GlobalFilter order=-2, injects X-Request-ID if absent
  security/
    JwtAuthenticationFilter.java  GlobalFilter order=-1, validates Bearer JWT, injects X-User-Id/X-User-Role
    JwtTokenValidator.java        RSA public key loader + JJWT RS256 parser
```

---

## Routes

| Route ID | Predicate | Upstream |
|---|---|---|
| student-profile-svc | `/api/v1/students/**` | `${STUDENT_PROFILE_SVC_URI}` |
| exam-tracker-svc | `/api/v1/exam-tracker/**` | `${EXAM_TRACKER_SVC_URI}` |
| performance-svc | `/api/v1/performance/**` | `${PERFORMANCE_SVC_URI}` |
| ai-mentor-svc-doubts | `/api/v1/doubts/**` | `${AI_MENTOR_SVC_URI}` |
| ai-mentor-svc-recommendations | `/api/v1/recommendations/**` | `${AI_MENTOR_SVC_URI}` |
| ai-mentor-svc-study-plans | `/api/v1/study-plans/**` | `${AI_MENTOR_SVC_URI}` |
| career-oracle-svc-profiles | `/api/v1/career-profiles/**` | `${CAREER_ORACLE_SVC_URI}` |
| career-oracle-svc-recommendations | `/api/v1/career-recommendations/**` | `${CAREER_ORACLE_SVC_URI}` |
| career-oracle-svc-colleges | `/api/v1/college-predictions/**` | `${CAREER_ORACLE_SVC_URI}` |
| mentor-svc-profiles | `/api/v1/mentors/**` | `${MENTOR_SVC_URI}` |
| mentor-svc-sessions | `/api/v1/mentor-sessions/**` | `${MENTOR_SVC_URI}` |

---

## Public Paths (no JWT required)

- `/actuator/**`
- `/swagger-ui/**`, `/v3/api-docs/**`, `/webjars/**`

All other paths require `Authorization: Bearer <JWT>`.

---

## Key Design Decisions

- **Same JWT key**: Uses the same RS256 public key as api-gateway — students authenticate via api-gateway/auth-svc, then use the same token here.
- **Header injection**: `X-User-Id` (from `sub` claim), `X-User-Role` (from `role` claim), `X-User-Center-Id` (from `centerId` claim if present).
- **Downstream trust**: All 6 EduPath services trust these headers — no JWT re-validation.
- **Rate limiting**: Redis token bucket via `RequestRateLimiter` default filter (env-var controlled).
- **Request ID**: `X-Request-ID` injected by `RequestIdFilter` (order -2) for distributed tracing.
- **Port**: `${STUDENT_GATEWAY_PORT}` (env-var, separate from main api-gateway port 8080).

---

## ArchUnit Rules (5)

1. `@ConfigurationProperties` classes reside in `config` package
2. `GlobalFilter` implementations reside in `security` or `filter` package
3. No `@Entity` (no JPA in a gateway)
4. No `@Service` — only `@Component` and `@Configuration`
5. `filter` package must not directly access `security` package internals

---

*This document is permanently frozen. Any future changes to student-gateway require a new versioned record.*
