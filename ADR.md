# Architecture Decision Records (ADR)
## EduTech AI Platform — school-APK

> **Repository:** https://github.com/Srikanthnexushub/school-APK
> **Date:** 2026-03-24
> **Status:** Living document — updated with every major architectural change

---

## Table of Contents

1. [ADR-001 — Hexagonal Architecture for all backend services](#adr-001)
2. [ADR-002 — RSA Asymmetric JWT (RS256) for authentication](#adr-002)
3. [ADR-003 — Single PostgreSQL instance with schema-per-service isolation](#adr-003)
4. [ADR-004 — Apache Kafka KRaft (no ZooKeeper) for event bus](#adr-004)
5. [ADR-005 — API Gateway pattern (two gateways: admin + student)](#adr-005)
6. [ADR-006 — React + Vite + TypeScript for frontend](#adr-006)
7. [ADR-007 — Zustand for global auth state](#adr-007)
8. [ADR-008 — React Query for server-side data fetching](#adr-008)
9. [ADR-009 — Flyway for database migrations](#adr-009)
10. [ADR-010 — MinIO (S3-compatible) for document/media storage](#adr-010)
11. [ADR-011 — Server-Sent Events (SSE) for real-time notifications](#adr-011)
12. [ADR-012 — TestContainers for integration testing (no DB mocks)](#adr-012)
13. [ADR-013 — Role hierarchy: SUPER_ADMIN → INSTITUTION_ADMIN → CENTER_ADMIN → TEACHER/PARENT/STUDENT](#adr-013)
14. [ADR-014 — OpenRouter as AI provider (model-agnostic gateway)](#adr-014)
15. [ADR-015 — Silent JWT refresh with device fingerprint binding](#adr-015)
16. [ADR-016 — pgvector for AI embedding storage](#adr-016)
17. [ADR-017 — Maven multi-module reactor build](#adr-017)
18. [ADR-018 — Framer Motion for UI animations](#adr-018)
19. [ADR-019 — DDL-restricted DB user + superuser migration pattern](#adr-019)
20. [ADR-020 — Observability: MDC Correlation IDs + Loki + OTel tracing + AOP logging](#adr-020)

---

## ADR-001 — Hexagonal Architecture for all backend services {#adr-001}

**Date:** 2025-Q3
**Status:** Accepted

### Context
We need consistent structure across 15 microservices that can be tested in isolation, with infrastructure concerns (JPA, Kafka, HTTP) swappable without touching business logic.

### Decision
All backend services follow Ports & Adapters (Hexagonal) architecture:
```
domain/        ← pure Java, no Spring annotations, no JPA
application/   ← use cases, DTOs, port interfaces
infrastructure/ ← Spring beans, JPA repositories, Kafka adapters
api/           ← Spring MVC controllers
```

### Consequences
- **+** Business logic is fully testable without Spring context
- **+** Infrastructure swap (e.g., Redis → Memcached) requires only new adapter
- **−** More boilerplate than simple layered architecture
- **Rule:** NEVER add `@Entity`, `@Repository`, or `@Service` annotations in `domain/` layer

---

## ADR-002 — RSA Asymmetric JWT (RS256) for authentication {#adr-002}

**Date:** 2025-Q3
**Status:** Accepted

### Context
With 15 microservices, symmetric (HS256) JWT would require sharing a secret across all services — a security risk. Services need to validate tokens independently without calling auth-svc on every request.

### Decision
- **auth-svc** holds the RSA private key and signs tokens
- All other services hold only the RSA public key and validate locally
- Token payload: `sub=userId`, `role`, `centerId`, `iat`, `exp`
- Access token TTL: **7200s (2 hours)** — extended from 15min (Fix #100)
- Refresh token TTL: **604800s (7 days)**, single-use with rotation

### Consequences
- **+** Services validate tokens without network hop to auth-svc
- **+** Private key never leaves auth-svc
- **−** Key rotation requires restarting all services
- **Key paths:** `keys/jwt-private.pem`, `keys/jwt-public.pem` — NEVER commit to git

---

## ADR-003 — Single PostgreSQL instance with schema-per-service isolation {#adr-003}

**Date:** 2025-Q3
**Status:** Accepted

### Context
Running 15 separate PostgreSQL instances is operationally expensive for a local dev / early-production deployment.

### Decision
Single PostgreSQL 16 instance with one schema per service:
- `auth_schema` (auth-svc)
- `center_schema` (center-svc)
- `assess_schema` (assess-svc)
- `mentor_schema` (mentor-svc)
- `parent_schema` (parent-svc)
- `psych_schema` (psych-svc)
- `student_schema` (student-profile-svc)
- `notification_schema` (notification-svc)

Each service's DB user has `USAGE` + `CREATE` on its own schema only. DDL (ALTER TABLE) requires superuser and is applied manually + recorded in flyway_schema_history.

### Consequences
- **+** Single instance to backup/restore
- **+** Cross-schema joins possible for reporting
- **−** One noisy neighbour can starve others (mitigated by HikariCP pool limits)
- **Production path:** Separate instances per service when load demands

---

## ADR-004 — Apache Kafka KRaft (no ZooKeeper) {#adr-004}

**Date:** 2025-Q3
**Status:** Accepted

### Context
Kafka 2.x required ZooKeeper, adding operational complexity. KRaft mode (Kafka 3.3+) is production-ready.

### Decision
- Image: `apache/kafka:3.7.0` (KRaft mode)
- Listeners: PLAINTEXT (9092, external), CONTROLLER (9093, KRaft), INTERNAL (29092, inter-container)
- Auto topic creation enabled for dev; disabled for production
- `CLUSTER_ID: sBhN79PnQiyZj7rDgLXtGA` — fixed for local dev, MUST change per environment

### Consequences
- **+** No ZooKeeper container required
- **+** Simpler single-node setup
- **−** KRaft cluster ID is env-specific — DO NOT reuse across prod/staging

---

## ADR-005 — Dual API Gateway pattern {#adr-005}

**Date:** 2025-Q3
**Status:** Accepted

### Context
Admin-facing and student-facing traffic have different RBAC requirements, rate limits, and downstream services. A single gateway creates a large, hard-to-reason-about routing table.

### Decision
- **api-gateway** (port 8180) — routes for admin, auth, center, parent, mentor, notification, assess, career, AI services
- **student-gateway** (port 8089) — routes specifically scoped for student-facing operations
- ALL frontend traffic MUST go through a gateway. Direct service calls are forbidden.

### Consequences
- **+** Clear separation of concerns
- **+** Different rate limiting profiles per gateway
- **−** Some endpoints duplicated in routing config

---

## ADR-006 — React + Vite + TypeScript + Tailwind CSS {#adr-006}

**Date:** 2025-Q3
**Status:** Accepted

### Decision
- **React 18** with functional components and hooks only
- **Vite** build tool (not CRA/webpack) — HMR, fast cold starts
- **TypeScript** strict mode — pre-commit hook blocks broken TS
- **Tailwind CSS** utility-first — no CSS modules or styled-components
- **Framer Motion** — page transitions and animated UI elements
- **React Router v6** — `useSearchParams` for tab state, `Outlet` for layout shell

### Key Rules
- `AppLayout.tsx` is the single shell for ALL roles — NEVER add a second sidebar inside a page
- NEVER use `-translate-x/y-1/2` with Framer Motion (it gets overridden by inline transform)
- ALWAYS use `<label>` wrapping `<input type="file" className="hidden">` for uploads

---

## ADR-007 — Zustand for global auth state {#adr-007}

**Date:** 2025-Q3
**Status:** Accepted

### Decision
Zustand with `persist` middleware stores auth state in `localStorage` under key `edupath-auth`.

State shape: `{ token, refreshToken, deviceId, user: {id, email, role, centerId, name}, isAuthenticated }`

`onRehydrateStorage`: clears expired access token on app load while preserving refresh token — silent refresh handles renewal on next API call.

### Consequences
- **+** Lightweight vs Redux
- **+** Works across page reloads
- **Rule:** `isAuthenticated: true` is CRITICAL in E2E token injection — without it ProtectedRoute redirects to /login

---

## ADR-008 — React Query (@tanstack/react-query) {#adr-008}

**Date:** 2025-Q3
**Status:** Accepted

### Decision
All server data fetching uses React Query. No manual `useEffect` + `useState` for API calls.

Key patterns:
- Spring `Page<T>` extraction: `Array.isArray(d) ? d : (d.content ?? [])`
- New-user 404 protection: `retry: false, throwOnError: false` on profile queries
- `enabled` guards prevent queries when required IDs are undefined

---

## ADR-009 — Flyway for database migrations {#adr-009}

**Date:** 2025-Q3
**Status:** Accepted

### Decision
Flyway manages all schema changes. Migration files: `db/migration/{schema}/V{N}__description.sql`

**DDL restriction pattern:** DB service users cannot ALTER TABLE (no DDL rights). DDL migrations (DROP/ADD column, CHECK constraint changes) must:
1. Run manually as superuser: `psql -U srikanth`
2. Insert checksum row into `flyway_schema_history` manually
3. Flyway then validates (not re-runs) on service startup

### Known DDL migrations requiring superuser:
- `chk_banner_type` CHECK constraint (V17/V18/V19/V23)
- NOT NULL drops on assignment marks (V12)
- pgvector extension activation

---

## ADR-010 — MinIO S3-compatible storage {#adr-010}

**Date:** 2025-Q3
**Status:** Accepted

### Decision
MinIO (S3-compatible) for document and media storage. Presigned URLs for browser-direct uploads:
- Upload: presigned PUT (15 min TTL)
- Download: presigned GET (5 min TTL)
- Bucket: `edutech-library`

Only `center-svc` interacts with MinIO (via `DocumentStoragePort` — hexagonal adapter).

**Local dev:** Native MinIO via Homebrew on port 9002 (API) / 9003 (console). Docker MinIO on 9000/9001 as backup.

---

## ADR-011 — Server-Sent Events (SSE) for real-time notifications {#adr-011}

**Date:** 2025-Q4
**Status:** Accepted

### Decision
SSE (not WebSocket) for real-time in-app notifications. Reasoning: SSE is unidirectional (server → client), simpler than WS for notification use case, and works through most proxies.

- Notification events published via Kafka
- notification-svc consumes and pushes to connected clients via SSE
- Client deduplicates by `id` to prevent badge inflation on SSE reconnect
- SMS fallback via Twilio (dev: `dev_placeholder` → log only)

---

## ADR-012 — TestContainers (real Postgres, no DB mocks) {#adr-012}

**Date:** 2025-Q4
**Status:** Accepted

### Decision
Integration tests use TestContainers to spin up real PostgreSQL (pgvector/pg16). No Mockito mocks for database layer.

**Critical config:** `pom.xml` maven-failsafe-plugin `<argLine>-Dapi.version=1.47</argLine>` — required for Docker Desktop 4.60+ (MinAPIVersion 1.44). NEVER remove.

Test pattern: `@SpringBootTest` + `@ActiveProfiles("test")` + static `PostgreSQLContainer`.

---

## ADR-013 — Role hierarchy {#adr-013}

**Date:** 2025-Q4
**Status:** Accepted

### Decision
```
SUPER_ADMIN (platform-level, pre-seeded, blocked from self-registration)
  └── INSTITUTION_ADMIN (self-registers as "Institution/Coaching Centre")
        └── CENTER_ADMIN (manages one center)
              └── TEACHER / PARENT / STUDENT (end users)
```

- `resolveAccessibleCenters()` — INSTITUTION_ADMIN sees only own centers; SUPER_ADMIN sees all
- `belongsToCenter()` — returns true for INSTITUTION_ADMIN and SUPER_ADMIN (bypass center check)
- SUPER_ADMIN self-registration is BLOCKED (`UserRegistrationService` throws 400)

---

## ADR-014 — OpenRouter as AI provider {#adr-014}

**Date:** 2025-Q4
**Status:** Accepted

### Decision
AI calls routed through `ai-gateway-svc` (port 8086) which abstracts the AI provider. Current provider: OpenRouter (`arcee-ai/trinity-large-preview:free`). Provider is switchable via `AI_DEFAULT_PROVIDER` env var without code changes.

Features: parent copilot, AI mentor (ai-mentor-svc), psychometric analysis, career oracle, AI project lab, staff bio generator.

---

## ADR-015 — Silent JWT refresh with device fingerprint binding {#adr-015}

**Date:** 2025-Q4
**Status:** Accepted

### Decision
- 401 interceptor in `api.ts` queues concurrent requests, exchanges refresh token for new pair silently
- Refresh token is single-use (UUID stored in Redis, deleted on rotation)
- Device fingerprint: `SHA-256(userAgent | deviceId | ipSubnet)` — fingerprint mismatch revokes ALL sessions for that user (security wipe)
- `isJwtExpired()` guard in interceptor prevents injecting stale tokens post-login (Fix #94)
- `CaptchaWidget` remounts on failed login via `captchaKey` state (Fix #94)

---

## ADR-016 — pgvector for AI embedding storage {#adr-016}

**Date:** 2025-Q4
**Status:** Accepted

### Decision
PostgreSQL `pgvector` extension used for storing and querying AI embeddings (psychometric profiles, career recommendations). Enables cosine similarity search (`<=>` operator) within existing PostgreSQL instance — no separate vector DB.

Custom Docker image: `Dockerfile.postgres` activates pgvector extension.

---

## ADR-017 — Maven multi-module reactor build {#adr-017}

**Date:** 2025-Q3
**Status:** Accepted

### Decision
Root `pom.xml` defines reactor with all 15 services + shared modules:
- `common-security` — shared JWT validation, RBAC helpers
- `event-contracts` — Kafka event schemas (shared between publishers and consumers)
- `test-fixtures` — shared TestContainers setup

**Rule:** ALWAYS run `mvn` from the project root. Internal modules only resolve from the local reactor (not published to any registry).

---

## ADR-018 — Framer Motion for UI animations {#adr-018}

**Date:** 2025-Q3
**Status:** Accepted

### Decision
Framer Motion for page transitions (`AnimatePresence`), animated tab indicators (`layoutId`), and modal entrance/exit.

**Critical gotcha:** Framer Motion sets `transform` inline, which overrides Tailwind `-translate-x/y-1/2`. Modal centering MUST use flexbox wrapper (`fixed inset-0 flex items-center justify-center`) + `createPortal`. NEVER use `-translate-x/y-1/2` with motion divs.

---

## ADR-019 — DDL-restricted service DB user + superuser migration pattern {#adr-019}

**Date:** 2025-Q4
**Status:** Accepted

### Decision
Service DB users have `SELECT/INSERT/UPDATE/DELETE` rights only — no `ALTER TABLE`, `DROP`, or `CREATE EXTENSION` DDL rights. This limits blast radius of an application-level SQL injection.

For DDL migrations:
1. Run `psql -U srikanth` (superuser)
2. Execute DDL manually
3. Insert row into `{schema}.flyway_schema_history` with correct checksum
4. Flyway validates on next service start (does not re-run)

---

## ADR-020 — Observability stack {#adr-020}

**Date:** 2026-Q1
**Status:** Accepted (Fix #85)

### Decision
Five layers of observability across all 15 services:
1. **MDC Correlation ID** — `X-Request-Id` header → `requestId` MDC key → every log line
2. **Loki log aggregation** — `loki4j-logback-appender` v1.5.2, Grafana datasource provisioned
3. **Distributed tracing** — `micrometer-tracing-bridge-otel`, TraceId in every log line
4. **AOP request logging** — `HttpRequestLoggingAspect` on all `@RestController` methods (entry/exit/duration)
5. **Log rate-limiting** — `DuplicateMessageFilter` TurboFilter prevents log flooding

**Rule:** NEVER use `@Slf4j` in filter/aspect classes — Lombok not in `annotationProcessorPaths`. Always use `LoggerFactory.getLogger()`.

---

*Last updated: 2026-03-24 | Commit: eb49b83*
