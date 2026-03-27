# EduTech AI Platform — Claude Code Session Guide

## ⛔ PERMISSION RULE — READ THIS FIRST
**ANY modification to ANY code, test, config, migration, or database requires EXPLICIT USER PERMISSION before acting.**
- Ask first. Act only after the user says yes. No exceptions — including "small" fixes and "obvious" improvements.
- NEVER declare a fix "done" without verifying end-to-end (DB → backend → API → frontend).
- Read `memory/frozen-fixes.md` before touching any file. 121+ frozen fixes as of Fix #121 (2026-03-25 — Attendance/Fees/Announcements modules, 10 pages, 3 ITs, router wiring).
- ⛔ Freezing ≠ verified. Test first, freeze after.

---

## 🧠 THINK BEFORE BUILDING

Before any new UI component, navigation, or feature — answer these first:
1. **Layout conflict**: Does `AppLayout.tsx` already render a sidebar/nav/header? Never add a second one.
2. **Data already exists?**: Is there an API, store, hook, or component that already does this? Reuse it.
3. **State location**: Local / Zustand / React Query?
4. **Role impact**: Does this affect CENTER_ADMIN / INSTITUTION_ADMIN / STUDENT / PARENT / TEACHER?
5. **Frozen conflict**: Does this touch any file in `frozen-fixes.md`? Ask permission explicitly.

---

## Start Services

> **ALL infrastructure is 100% NATIVE** — Docker Desktop can be OFF.
> Postgres, Redis, Kafka, MinIO, MailHog, Kafka UI, Redis Commander all run natively via Homebrew/LaunchAgents.

```bash
brew services start postgresql@16          # ensure Postgres is up (after reboot)
bash scripts/start-all.sh --no-build       # smart start (auto-rebuilds changed services)
bash scripts/start-all.sh                  # full build + start
```

**Redis down (CAPTCHA broken):**
```bash
brew services restart redis
redis-cli -a <REDIS_PASSWORD> ping   # should return PONG
```

For all other startup problems → `memory/startup-troubleshooting.md`
Infrastructure details → `memory/quick-ref-infra.md`

---

## Service Ports

| Service | Port | Notes |
|---|---|---|
| **Frontend (Vite)** | **3000** | |
| **api-gateway** | **8180** | All frontend traffic |
| **student-gateway** | **8089** | Student-facing routes |
| auth-svc | 8182 | Tomcat WAR |
| center-svc | 8083 | Tomcat WAR |
| parent-svc | 8082 | Tomcat WAR |
| assess-svc | 8084 | Tomcat WAR |
| mentor-svc | 8088 | Tomcat WAR |
| student-profile-svc | 8090 | Tomcat WAR |
| notification-svc | 8094 | Tomcat WAR |
| exam-tracker-svc | 8091 | Tomcat WAR |
| career-oracle-svc | 8087 | Tomcat WAR |
| performance-svc | 8092 | Tomcat WAR |
| psych-svc | 8085 | Tomcat WAR |
| ai-gateway-svc | 8086 | Exec WAR — standalone Netty (NOT Tomcat, see Fix #103) |

Health check: `curl http://localhost:{port}/actuator/health`
Tomcat deployment details → `memory/tomcat-deployment.md`

---

## Infrastructure Ports (all native — Docker Desktop NOT required)

| Service | Port | Start command |
|---|---|---|
| Postgres | 5432 | `brew services start postgresql@16` |
| Redis | 6379 | `brew services start redis` |
| Kafka | 9092 | `launchctl load ~/Library/LaunchAgents/com.edutech.kafka-native.plist` |
| MinIO API | 9002 | `launchctl load ~/Library/LaunchAgents/com.edutech.minio-native.plist` |
| MinIO Console | 9003 | (same plist) |
| MailHog SMTP | 1025 | `launchctl load ~/Library/LaunchAgents/com.edutech.mailhog-native.plist` |
| MailHog UI | 8025 | (same plist) |
| Kafka UI | 9080 | `launchctl load ~/Library/LaunchAgents/com.edutech.kafka-ui-native.plist` |
| Redis Commander | 8888 | `launchctl load ~/Library/LaunchAgents/com.edutech.redis-commander-native.plist` |

---

## Test Users (password: `Test@12345`)

| Role | Email | Notes |
|---|---|---|
| **Parent** | `ravi.parent@test.com` | userId `bd7d02da-11e3-4bda-9795-41d3c93bac69` |
| **Student** | `qa-test@nexused.dev` | userId `50d63f9c-9b9c-4737-a66a-22b29dad42a1` |
| **Teacher** | `teacher1@test.com` | |
| **CENTER_ADMIN** | `institute@nexused.com` | centerId `6e9985dd-f029-49aa-8d22-39c42525df97` |
| **INSTITUTION_ADMIN** | `superadmin@nexused.com` | platform admin, no centerId |

Full credentials + E2E demo data → `memory/quick-ref-auth-users.md`, `memory/e2e-demo-data.md`

---

## Architecture Rules (NEVER violate)

- **Hexagonal**: domain → application → infrastructure. No Spring annotations in `domain/`.
- **JWT**: every service validates with RSA public key. 15-min TTL. All inter-service calls require JWT header.
- **Gateways only**: frontend → api-gateway (8180) or student-gateway (8089). Never call services directly.
- **Captcha bypass** (E2E/dev): `captchaToken: "E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD:bypass"`
- **Page<T>**: `Array.isArray(d) ? d : (d.content ?? [])`
- **Tomcat WAR**: 11 services run as Tomcat WARs. api-gateway + student-gateway on Netty (spring-cloud-gateway). ai-gateway-svc runs as exec WAR (standalone Netty) — NOT Tomcat.

Patterns → `memory/frontend-patterns.md`, `memory/project-architecture.md`, `memory/tomcat-deployment.md`

---

## Key Features — All Frozen

Full list → `memory/frozen-fixes.md` (123 fixes, latest: Fix #122–#123 — banner write restricted to SUPER_ADMIN only; Accounts section + Billing report + parent fee reminders via Kafka IN_APP notifications).
Read it before touching any existing file.

### ⚠️ PERMANENT RULES FROM E2E BUG SWEEP (2026-03-25)

**DB migrations**: Every enum value addition in Java MUST have a corresponding Flyway migration in the SAME commit — never rely on manual DB changes. If you add a status to any enum that maps to a DB CHECK constraint, update the constraint in a new `V{N}__*.sql` migration file simultaneously.

**Gateway routes**: When a new endpoint is added to any service that serves paths under `/api/v1/students/**`, you MUST add a specific route in `student-gateway/application.yml` BEFORE the `Path=/api/v1/students/**` catch-all. First-match-wins.

**Flyway + DB ownership (this dev DB)**: Tables in `assess_db`, `auth_db`, etc. are owned by `srikanth` (superuser) because migrations ran as srikanth during initial setup. Future DDL migrations that use `ALTER TABLE` on these schemas must be run manually as `postgres`/`srikanth` superuser first, then inserted into `flyway_schema_history`. Fresh DBs are not affected (tables will be owned by the app user).

**start-all.sh find**: All `find` calls in `start_svc()` use `-maxdepth 1` — never remove this. WAR packaging creates exploded `WEB-INF/lib/` inside `target/` and without depth limiting, the first match is a dependency JAR, not the service artifact.

---

## Env File

`.env` in project root. Load: `while IFS='=' read -r key val; do [[ -z "$key" || "$key" == \#* ]] && continue; export "$key=$val"; done < .env`
- `AI_DEFAULT_PROVIDER=OPENROUTER` (arcee-ai/trinity-large-preview:free)
- `TWILIO_ACCOUNT_SID=dev_placeholder` → SMS logs only
- `CAPTCHA_E2E_BYPASS_TOKEN=E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD`
