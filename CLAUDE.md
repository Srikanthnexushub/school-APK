# EduTech AI Platform — Claude Code Session Guide

## ⛔ PERMISSION RULE — READ THIS FIRST
**ANY modification to ANY code, test, config, migration, or database requires EXPLICIT USER PERMISSION before acting.**
- Ask first. Act only after the user says yes. No exceptions — including "small" fixes and "obvious" improvements.
- NEVER declare a fix "done" without verifying end-to-end (DB → backend → API → frontend).
- Read `memory/frozen-fixes.md` before touching any file. 100+ frozen fixes as of Fix #100 (commit `4dcff0b`).
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

> **Postgres runs LOCALLY** via Homebrew `postgresql@16` — NOT in Docker.

```bash
brew services start postgresql@16          # ensure Postgres is up (after reboot)
bash scripts/start-all.sh --no-build       # smart start (auto-rebuilds changed services)
bash scripts/start-all.sh                  # full build + start
bash scripts/start-all.sh --infra-only     # Docker infra only (Redis/Kafka)
```

**Redis down (CAPTCHA broken):**
```bash
while IFS='=' read -r key val; do [[ -z "$key" || "$key" == \#* ]] && continue; export "$key=$val"; done < .env
docker compose -f infrastructure/docker/docker-compose.yml up -d redis
```

For all other startup problems → `memory/startup-troubleshooting.md`

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
| ai-gateway-svc | 8086 | Tomcat WAR (WebFlux+Netty client) |
| exam-tracker-svc | 8092 | Tomcat WAR |
| career-oracle-svc | 8095 | Tomcat WAR |
| performance-svc | 8096 | Tomcat WAR |
| psych-svc | 8097 | Tomcat WAR |

Health check: `curl http://localhost:{port}/actuator/health`
Tomcat deployment details → `memory/tomcat-deployment.md`

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
- **Tomcat WAR**: 12 services run as WARs. api-gateway + student-gateway stay on embedded Netty (spring-cloud-gateway).

Patterns → `memory/frontend-patterns.md`, `memory/project-architecture.md`, `memory/tomcat-deployment.md`

---

## Key Features — All Frozen

Full list → `memory/frozen-fixes.md` (100 fixes, latest: Fix #100 `4dcff0b` — Tomcat WAR migration).
Read it before touching any existing file.

---

## Env File

`.env` in project root. Load: `while IFS='=' read -r key val; do [[ -z "$key" || "$key" == \#* ]] && continue; export "$key=$val"; done < .env`
- `AI_DEFAULT_PROVIDER=OPENROUTER` (arcee-ai/trinity-large-preview:free)
- `TWILIO_ACCOUNT_SID=dev_placeholder` → SMS logs only
- `CAPTCHA_E2E_BYPASS_TOKEN=E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD`
