# Product Requirements Document (PRD)
## EduTech AI Platform — school-APK

> **Repository:** https://github.com/Srikanthnexushub/school-APK
> **Date:** 2026-03-24
> **Version:** 1.0.0-SNAPSHOT → Release Candidate
> **Owner:** Srikanth (NexusHub)

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [Phase 1 — Design & Dev Foundations](#2-phase-1--design--dev-foundations)
3. [Phase 2 — Coding](#3-phase-2--coding)
4. [Phase 3 — Hardening](#4-phase-3--hardening)
5. [Phase 4 — Shipping](#5-phase-4--shipping)
6. [Role Matrix](#6-role-matrix)
7. [Feature Inventory](#7-feature-inventory)
8. [API Surface Summary](#8-api-surface-summary)
9. [Non-Functional Requirements](#9-non-functional-requirements)
10. [Known Constraints & Risks](#10-known-constraints--risks)

---

## 1. Product Overview

### 1.1 Vision
EduTech AI Platform is a multi-tenant, AI-powered school and coaching centre management system. It enables institutions (schools, colleges, coaching centres) to manage students, teachers, assessments, assignments, psychometric profiling, career guidance, and parent engagement — all from a single platform.

### 1.2 Target Users

| Role | Description |
|---|---|
| **SUPER_ADMIN** | Platform owner; manages all institutions; pre-seeded |
| **INSTITUTION_ADMIN** | Institution owner; self-registers; manages 1–N centers |
| **CENTER_ADMIN** | Center head; manages teachers, batches, students, fees |
| **TEACHER** | Delivers content, grades assignments, manages sessions |
| **STUDENT** | Takes exams, submits assignments, uses AI mentor |
| **PARENT** | Monitors child progress, uses AI copilot, pays fees |

### 1.3 Platform Scope
- **15 microservices** (Spring Boot 3 / Java 17)
- **1 frontend** (React 18 / TypeScript / Vite)
- **2 API gateways** (api-gateway:8180, student-gateway:8089)
- **Shared infra**: PostgreSQL 16, Redis 7, Apache Kafka 3.7, MinIO, MailHog

---

## 2. Phase 1 — Design & Dev Foundations

### 2.1 Architecture Foundations (COMPLETED)

| Component | Decision | Status |
|---|---|---|
| Backend pattern | Hexagonal (Ports & Adapters) | ✅ Done |
| Auth | RSA JWT (RS256), 2h access / 7d refresh | ✅ Done |
| DB isolation | Schema-per-service on single Postgres | ✅ Done |
| Event bus | Kafka KRaft 3.7 (no ZooKeeper) | ✅ Done |
| Storage | MinIO S3-compatible (presigned URLs) | ✅ Done |
| Frontend shell | AppLayout.tsx single shell, role-based nav | ✅ Done |
| State management | Zustand (auth) + React Query (server data) | ✅ Done |
| Routing | React Router v6, URL-based tab state | ✅ Done |

### 2.2 Dev Environment Setup

**Prerequisites:**
```bash
# macOS
brew install postgresql@16 maven node openjdk@17
brew services start postgresql@16

# Docker Desktop (for Redis/Kafka/MailHog/MinIO containers)
# Download from: https://www.docker.com/products/docker-desktop/
```

**First-time setup:**
```bash
git clone https://github.com/Srikanthnexushub/school-APK.git
cd school-APK
cp .env.example .env        # fill in DB passwords, JWT key paths
bash scripts/local-dev-setup.sh   # creates DBs, schemas, users
bash scripts/start-all.sh          # builds + starts all services
```

**Daily startup:**
```bash
brew services start postgresql@16
bash scripts/start-all.sh --no-build   # skip rebuild if source unchanged
```

### 2.3 Design System

| Element | Technology | Notes |
|---|---|---|
| UI framework | React 18 + TypeScript | Strict mode |
| Styling | Tailwind CSS | No CSS modules |
| Animations | Framer Motion | NEVER use `-translate-x/y-1/2` with motion divs |
| Icons | Lucide React | Consistent icon library |
| Charts | Recharts / Victory | Performance graphs, radar charts |
| Forms | Controlled React state | No form libraries |
| Modals | createPortal | Escapes overflow:hidden |
| Color scheme | Indigo/Purple primary, role-specific accents | |

### 2.4 Database Design Principles

- Schema-per-service isolation
- All foreign keys validated at application layer (no cross-schema FK constraints)
- UUIDs as primary keys (`gen_random_uuid()`)
- `created_at`, `updated_at` on all tables
- Soft deletes via `deleted_at` nullable column
- pgvector activated in assess_db and psych_db for AI embeddings

---

## 3. Phase 2 — Coding

### 3.1 Backend Coding Standards

**Service structure (strictly enforced):**
```
services/{service-name}/
  src/main/java/com/edutech/{name}/
    api/               ← @RestController classes
    application/
      dto/             ← Request/Response records
      service/         ← Use case implementations
      port/            ← Port interfaces (in/out)
    domain/
      model/           ← Pure Java domain objects
      event/           ← Domain events
    infrastructure/
      jpa/             ← @Entity, @Repository
      kafka/           ← Kafka producers/consumers
      redis/           ← Redis adapters
  src/main/resources/
    db/migration/{schema}/   ← Flyway migrations
    application.yml
    logback-spring.xml
```

**Rules:**
- No Spring annotations in `domain/`
- All inter-service calls include `Authorization: Bearer {token}` header
- RBAC enforced at service layer, NOT just gateway
- Empty-body 403 = Jackson deserialization failure (bad enum value), NOT auth error
- JPQL: use `CAST(:param AS String)` for nullable string params (prevents `lower(bytea)` error)

**New migration checklist:**
1. Increment V number: check `SELECT MAX(version) FROM flyway_schema_history`
2. DDL (ALTER TABLE, CREATE INDEX): requires superuser + manual flyway row
3. Enum constraint changes: DROP+RECREATE `chk_` constraint

### 3.2 Frontend Coding Standards

**File structure:**
```
frontend/web/src/
  components/
    layout/           ← AppLayout.tsx (SINGLE shell, do not duplicate)
    ui/               ← Reusable components
    routing/          ← ProtectedRoute.tsx
  pages/
    admin/            ← CENTER_ADMIN / INSTITUTION_ADMIN / SUPER_ADMIN
    auth/             ← Login, Register
    mentor-portal/    ← TEACHER pages
    parent/           ← PARENT pages
    student/          ← STUDENT default
    {feature}/        ← Feature-specific pages
  stores/             ← Zustand stores
  lib/                ← api.ts (axios instance + interceptors)
  utils/              ← helpers, indiaLocations.ts
  hooks/              ← useNotifications, etc.
```

**Rules:**
- Before adding any UI: check AppLayout.tsx for existing nav/sidebar
- Page<T> extraction: `Array.isArray(d) ? d : (d.content ?? [])`
- Profile queries: `retry: false, throwOnError: false` (may 404 for new users)
- File uploads: `<label>` wrapping `<input type="file" className="hidden">` ALWAYS
- SSE notifications: dedup by `id` in useNotifications hook
- NEVER hardcode role names in UI labels — derive from centerType field

### 3.3 Feature Implementation Checklist

For every new feature:
- [ ] Backend: domain model → port interface → service → controller → Flyway migration
- [ ] RBAC: which roles can access? Verified at service layer
- [ ] Frontend: read AppLayout.tsx first for nav integration
- [ ] IT test: TestContainers-based `@SpringBootTest` covering happy path + RBAC
- [ ] E2E test: Playwright spec in `frontend/web/tests/e2e/`
- [ ] Freeze in CLAUDE.md + memory after verification

### 3.4 Implemented Features (100 Fixes, all frozen)

See [Feature Inventory](#7-feature-inventory) below for complete list. All 100 features are frozen — modification requires explicit permission.

---

## 4. Phase 3 — Hardening

### 4.1 Security Hardening

#### Authentication & Authorization
| Requirement | Implementation | Status |
|---|---|---|
| Password hashing | Argon2id | ✅ |
| JWT signing | RSA-2048 (RS256) | ✅ |
| Access token TTL | 7200s (2h) | ✅ Fix #100 |
| Refresh token rotation | Single-use UUID, Redis-backed | ✅ |
| Device fingerprint | SHA-256(UA+deviceId+subnet) | ✅ |
| Fingerprint mismatch | Revoke ALL sessions for user | ✅ |
| CAPTCHA | Redis-backed challenge/response | ✅ |
| Stale token interceptor | `isJwtExpired()` guard | ✅ Fix #94 |
| Social login | GitHub OAuth + Google OAuth | ✅ Fix #91 |
| SUPER_ADMIN self-reg | Blocked (throws 400) | ✅ |
| CORS | Configured per gateway | ✅ |
| HTTPS | Required for production (see Phase 4) | ⬜ |

#### Database Security
| Requirement | Implementation | Status |
|---|---|---|
| DB user DDL restriction | Service users: no ALTER/DROP/CREATE | ✅ ADR-019 |
| Schema isolation | Cross-service schema reads blocked | ✅ |
| Connection pooling | HikariCP with pool limits | ✅ |
| SQL injection | Spring Data JPA + JPQL (no raw SQL) | ✅ |

#### Application Security
| Requirement | Implementation | Status |
|---|---|---|
| Input validation | Bean Validation (`@Valid`, `@NotBlank`) | ✅ Fix #100 |
| XSS prevention | React's JSX escaping + Tailwind (no `dangerouslySetInnerHTML`) | ✅ |
| RBAC | Per-endpoint role checks in service layer | ✅ |
| Secrets management | `.env` file (not committed) | ✅ |
| Pre-commit hook | TypeScript check blocks broken commits | ✅ |

### 4.2 Observability (Fix #85 — all 15 services)

| Layer | Tool | Coverage |
|---|---|---|
| Correlation IDs | MDC `requestId` (`X-Request-Id` header) | 15/15 services |
| Structured logging | Logback JSON + `[rid=][tid=]` format | 15/15 services |
| Log aggregation | Loki (`loki4j-logback-appender` v1.5.2) | 15/15 services |
| Distributed tracing | OTel (`micrometer-tracing-bridge-otel`) | 15/15 services |
| Request/response logs | `HttpRequestLoggingAspect` (AOP) | 13 servlet + 2 reactive |
| Log rate-limiting | `DuplicateMessageFilter` TurboFilter | 15/15 services |
| Dashboards | Grafana + Loki datasource | Provisioned |

### 4.3 Integration Test Coverage

| Service | Test Class | Tests |
|---|---|---|
| auth-svc | `AuthControllerIT` | 12+ |
| assess-svc | `AssignmentControllerIT` | 17 |
| center-svc | `BannerControllerIT` | 20 |
| center-svc | `JobPostingControllerIT` | 17 |
| psych-svc | `PsychControllerIT` + `PsychAssessmentIT` | 17 |
| **Total** | | **85+** |

All IT tests use TestContainers (real PostgreSQL). No DB mocks.

### 4.4 E2E Test Coverage (Playwright)

| Spec | Suites | Tests |
|---|---|---|
| `institution-portal.spec.ts` | 8 | 37 |
| `login-page-showcase.spec.ts` | 3 | 17 |
| `student-onboarding.spec.ts` | 2 | 5 |
| `job-board.spec.ts` | 3 | 14 |
| `library.spec.ts` | — | — |
| **Total** | | **73+** |

### 4.5 Performance Baseline

| Metric | Target | Notes |
|---|---|---|
| API P95 response time | < 500ms | Single-server baseline |
| Frontend TTI (Time to Interactive) | < 3s | Vite code splitting |
| DB connection pool | 10 per service | HikariCP |
| Kafka consumer lag | < 1000 messages | Per consumer group |
| Log watchdog | 100MB cap per log file | 10min interval via start-all.sh |

### 4.6 Disaster Recovery

| Scenario | Recovery Steps |
|---|---|
| Postgres down | `brew services start postgresql@16` |
| Redis/Docker crash | Restart Docker Desktop → `docker compose up -d redis` |
| Stale JAR after commit | `bash scripts/start-all.sh --no-build` (auto-detects + rebuilds) |
| Disk full (ENOSPC) | `bash scripts/cleanup.sh --force` |
| pom.xml corruption | Remove `dumb` prefix if present |
| JWT expired | Re-login (silent refresh handles it if refresh token present) |

---

## 5. Phase 4 — Shipping

### 5.1 Pre-Ship Checklist

#### Security
- [ ] Rotate all `.env` secrets (DB passwords, JWT keys, MinIO keys)
- [ ] Generate new RSA key pair for JWT (production keys, not dev keys)
- [ ] Set `JWT_ISSUER` to production domain
- [ ] Disable `CAPTCHA_E2E_BYPASS_TOKEN` (remove or set to empty)
- [ ] Enable HTTPS on all gateways (TLS termination at load balancer)
- [ ] Set `CORS_ALLOWED_ORIGINS` to production frontend domain only
- [ ] Disable Kafka UI, pgAdmin, Redis Commander, MailHog in production docker-compose
- [ ] Change MinIO root credentials from defaults (`minioadmin/minioadmin123`)
- [ ] Set `TWILIO_ACCOUNT_SID` and `TWILIO_AUTH_TOKEN` to real values for SMS
- [ ] Set `GITHUB_OAUTH2_CLIENT_ID/SECRET` and `VITE_GITHUB_CLIENT_ID` for GitHub OAuth
- [ ] Set `VITE_GOOGLE_CLIENT_ID` for Google Sign-In
- [ ] Review all `@CrossOrigin` annotations — remove any `*` wildcards
- [ ] Disable `KAFKA_AUTO_CREATE_TOPICS_ENABLE` in production

#### Infrastructure
- [ ] PostgreSQL: dedicated instance per service (or RDS)
- [ ] Redis: Redis Sentinel or Redis Cluster for HA
- [ ] Kafka: 3-broker cluster (not single-node)
- [ ] MinIO: distributed mode (4+ nodes) or AWS S3
- [ ] Set `KAFKA_DEFAULT_REPLICATION_FACTOR=3` (was 1 for dev)
- [ ] Enable Kafka SSL listeners (SASL_SSL protocol)
- [ ] PostgreSQL: enable `pg_hba.conf` for network access control

#### Application
- [ ] Set `spring.profiles.active=prod` (disables H2 console, enables prod configs)
- [ ] Set `KAFKA_CLUSTER_ID` to a new UUID (never reuse dev cluster ID)
- [ ] Verify all 15 services pass health checks: `/actuator/health`
- [ ] Run full IT test suite: `mvn verify -Pit`
- [ ] Run E2E suite: `npx playwright test`
- [ ] Verify Flyway migrations applied cleanly on fresh DB
- [ ] Performance test: 100 concurrent users

#### Frontend
- [ ] `npm run build` — verify zero TS errors
- [ ] Set `VITE_API_BASE_URL` to production API gateway URL
- [ ] Remove `?tab=debug` and any dev-only routes from router
- [ ] Set `Content-Security-Policy` headers
- [ ] Verify `robots.txt` and `sitemap.xml`

### 5.2 Deployment Architecture (Production)

```
Internet
    │
    ▼
[Load Balancer / Nginx]
    │         │
    ▼         ▼
[api-gateway] [student-gateway]
    │               │
    ▼               ▼
[auth-svc] [center-svc] [assess-svc] [mentor-svc]
[parent-svc] [psych-svc] [notification-svc]
[ai-gateway-svc] [ai-mentor-svc] [career-oracle-svc]
[student-profile-svc] [exam-tracker-svc] [performance-svc]
    │
    ▼
[PostgreSQL Cluster] [Redis Sentinel] [Kafka Cluster]
[MinIO / S3]         [Loki + Grafana]
```

### 5.3 Docker Compose Production Override

```yaml
# docker-compose.prod.yml (create this file — never commit secrets)
services:
  kafka:
    environment:
      KAFKA_DEFAULT_REPLICATION_FACTOR: 3
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"
  # Remove: kafka-ui, pgadmin, redis-commander, mailhog
```

### 5.4 Service Start Order

```
1. PostgreSQL (external or managed)
2. Redis
3. Kafka
4. MinIO / S3
5. auth-svc           ← all others depend on JWT validation
6. center-svc
7. student-profile-svc, parent-svc, mentor-svc, psych-svc
8. assess-svc, exam-tracker-svc, performance-svc, career-oracle-svc
9. notification-svc   ← depends on Kafka consumers from above
10. ai-gateway-svc, ai-mentor-svc
11. api-gateway, student-gateway
12. Frontend (Nginx static serve or CDN)
```

### 5.5 Rollback Plan

```bash
# Tag release before deploying
git tag v1.0.0-rc1
git push origin v1.0.0-rc1

# Rollback: checkout previous tag, rebuild JARs, restart
git checkout v0.9.x
bash scripts/start-all.sh
```

### 5.6 Monitoring & Alerting (Post-Ship)

| Signal | Tool | Alert Threshold |
|---|---|---|
| Service health | `/actuator/health` polling | DOWN for > 30s |
| API error rate | Loki query on `ERROR` logs | > 1% of requests |
| API P95 latency | Grafana + OTel traces | > 1000ms |
| Kafka consumer lag | Kafka UI / JMX | > 10,000 messages |
| Disk space | OS monitoring | > 80% |
| Memory | JVM heap metrics via actuator | > 85% heap |

---

## 6. Role Matrix

| Feature | SUPER_ADMIN | INSTITUTION_ADMIN | CENTER_ADMIN | TEACHER | STUDENT | PARENT |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| Create/manage centers | ✅ | ✅ (own) | — | — | — | — |
| Manage staff | ✅ | ✅ | ✅ | — | — | — |
| Manage batches | ✅ | ✅ | ✅ | — | — | — |
| Post job listings | ✅ | ✅ | ✅ | — | — | — |
| View job board | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Create assignments | ✅ | ✅ | ✅ | ✅ | — | — |
| Submit assignments | — | — | — | — | ✅ | — |
| Grade assignments | ✅ | ✅ | ✅ | ✅ | — | — |
| Create exams | ✅ | ✅ | ✅ | ✅ | — | — |
| Take exams | — | — | — | — | ✅ | — |
| View psychometric data | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ (own child) |
| AI copilot | — | — | — | — | — | ✅ |
| AI mentor | — | — | — | — | ✅ | — |
| Career oracle | — | — | — | — | ✅ | — |
| Library upload | ✅ | ✅ | ✅ | — | — | — |
| Library view | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| Manage banners | ✅ | ✅ | — | — | — | — |
| View banners | ✅ | ✅ | ✅ | — | — | ✅ |
| Notifications (receive) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Register child | — | — | — | — | — | ✅ |
| Fee management | ✅ | ✅ | ✅ | — | — | ✅ (view) |

---

## 7. Feature Inventory

All 100 features are frozen. Modifications require explicit permission.

### Core Platform
| # | Feature | Commit |
|---|---|---|
| 1 | Multi-role auth (STUDENT/PARENT/TEACHER/CENTER_ADMIN/INSTITUTION_ADMIN/SUPER_ADMIN) | 7f01e5d |
| 2 | JWT RSA RS256, 2h access / 7d refresh tokens | eb49b83 |
| 3 | CAPTCHA challenge/response (Redis-backed) | b54547a |
| 4 | Google OAuth sign-in | 944f801 |
| 5 | GitHub OAuth sign-in | fdb565e |
| 6 | OTP-based parent-child linking | ac81927 |
| 7 | Password recovery + MFA/TOTP | 5bd149f |
| 8 | Silent JWT refresh with device fingerprint | 4bf1131 |
| 9 | Real-time SSE notifications + SMS (Twilio) | 275e718 |
| 10 | Profile completion ring (role-specific fields) | dc33618 |

### Institution Management
| # | Feature | Commit |
|---|---|---|
| 11 | Center creation (COACHING_CENTER/SCHOOL/COLLEGE) | e843293 |
| 12 | Staff portal (8 role types, AI bio generator) | various |
| 13 | Bulk teacher import | 8bd6075 |
| 14 | Batch management | 7f01c7b |
| 15 | Job postings (CRUD + public board) | 1b19097 |
| 16 | Advertisement banners (HERO/TICKER/VIDEO/FOOTER_VIDEO) | various |
| 17 | Library (document upload, presigned MinIO) | 4895229 |

### Assessment
| # | Feature | Commit |
|---|---|---|
| 18 | Exam creation + CAT/STANDARD modes | d301e53 |
| 19 | Assignments (HOMEWORK/CLASSWORK/PROJECT/QUIZ/PRACTICE) | d301e53 |
| 20 | Assignment state machine (DRAFT→PUBLISHED→CLOSED) | d301e53 |
| 21 | Late submission detection | d301e53 |
| 22 | Assignment grading | d301e53 |

### AI Features
| # | Feature | Commit |
|---|---|---|
| 23 | Parent AI Copilot (psychometric-aware) | various |
| 24 | AI Mentor for students | various |
| 25 | Psychometric profiling (Big Five + RIASEC + Learning Style) | 2666427 |
| 26 | Career Oracle recommendations | various |
| 27 | AI Project Lab | c20394d |

### Student/Parent Journeys
| # | Feature | Commit |
|---|---|---|
| 28 | Student dashboard + profile completion | 78fb9a5 |
| 29 | Parent dashboard + children management | 6c45f2d |
| 30 | Job Board (read-only, all roles) | 93703a4 |
| 31 | India location autocomplete + pincode auto-fill | bf69e3d |
| 32 | Profile edit forms (all roles) | 6e91249 |

---

## 8. API Surface Summary

### Gateways

| Gateway | Port | Services Routed |
|---|---|---|
| api-gateway | 8180 | auth, center, assess, mentor, parent, notification, ai-gateway, psych, student-profile, career-oracle, performance, exam-tracker |
| student-gateway | 8089 | student-profile, assess, psych, ai-mentor, career-oracle |

### Key Endpoints

| Method | Path | Service | Auth |
|---|---|---|---|
| POST | `/api/v1/auth/login` | auth-svc | Public |
| POST | `/api/v1/auth/register` | auth-svc | Public |
| POST | `/api/v1/auth/refresh` | auth-svc | Public |
| POST | `/api/v1/auth/github` | auth-svc | Public |
| GET | `/api/v1/centers` | center-svc | Any auth |
| POST | `/api/v1/centers` | center-svc | SUPER_ADMIN |
| GET | `/api/v1/assignments` | assess-svc | Role-filtered |
| POST | `/api/v1/assignments` | assess-svc | ADMIN/TEACHER |
| GET | `/api/v1/jobs` | center-svc | Any auth |
| GET | `/api/v1/banners/active` | center-svc | Any auth |
| GET | `/api/v1/notifications` | notification-svc | Any auth |
| POST | `/api/v1/psych/profiles` | psych-svc | Any auth |

---

## 9. Non-Functional Requirements

### 9.1 Security
- All traffic over HTTPS in production
- JWT access tokens 2h TTL, refresh tokens 7 days
- Argon2id password hashing
- No secrets in code or git history
- RBAC at both gateway AND service layer
- Rate limiting on auth endpoints

### 9.2 Availability
- Target: 99.5% monthly uptime (single-server baseline)
- Health endpoints: `/actuator/health` on all services
- Graceful shutdown: Spring's `server.shutdown=graceful`
- Auto-restart: managed by `systemd` or Docker restart policy

### 9.3 Scalability
- Horizontal: stateless services scale independently
- Session state: stored in Redis (not in-process)
- DB: schema-per-service enables per-service scaling
- Kafka: partitioned topics allow parallel consumer scaling

### 9.4 Maintainability
- Hexagonal architecture: infrastructure swappable
- Flyway migrations: reproducible schema evolution
- TestContainers: no environment-specific test configuration
- ADR.md: all architectural decisions documented
- CLAUDE.md: all frozen fixes documented

---

## 10. Known Constraints & Risks

| Constraint | Impact | Mitigation |
|---|---|---|
| DDL-restricted DB user | Migrations need superuser | Document pattern in ADR-019 |
| Loki not running in dev | Log aggregation errors | Non-fatal; logs still go to files |
| MinIO credentials in `.env` | Dev-only issue | Rotate before production |
| Kafka single-node | No HA | Use 3-broker cluster in production |
| JWT keys on filesystem | Key rotation requires restart | Use secrets manager in production |
| TestContainers needs Docker | CI requires Docker-in-Docker | Use DinD GitHub Actions runner |
| 15-min legacy JWT TTL in memory | Some docs still say 15 min | Updated to 2h in Fix #100 |
| Teacher library upload (backend) | Backend allows, frontend has no UI | Decision pending (#101) |

---

*Last updated: 2026-03-24 | Repository: https://github.com/Srikanthnexushub/school-APK*
