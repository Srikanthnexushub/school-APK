# EduTech AI Platform — Claude Code Session Guide

## ⛔ PERMISSION RULE — READ THIS FIRST
**ANY modification to ANY code, test, config, migration, or database requires EXPLICIT USER PERMISSION before acting.**
- Ask first. Act only after the user says yes. No exceptions — including "small" fixes and "obvious" improvements.
- NEVER declare a fix "done" without verifying end-to-end (DB → backend → API → frontend).
- Read `memory/frozen-fixes.md` before touching any file. 212+ frozen fixes as of Fix #212 (2026-04-02 — Nexus AI forced white text + mobile chat overflow + guardian chip all-screen).
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
| nexus-chat-svc | 8097 | Tomcat WAR |
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
| **Teacher** | `teacher1@test.com` | local dev only; EC2 live teacher: `sri.teacher@school.com` |
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

Full list → `memory/frozen-fixes.md` (212 fixes, latest: Fix #212 — Nexus AI forced white text + mobile chat responsive sizing + guardian chip visible on all screens (2026-04-02)).
Read it before touching any existing file.

### ⚠️ PERMANENT RULES FROM E2E BUG SWEEP (2026-03-25)

**DB migrations**: Every enum value addition in Java MUST have a corresponding Flyway migration in the SAME commit — never rely on manual DB changes. If you add a status to any enum that maps to a DB CHECK constraint, update the constraint in a new `V{N}__*.sql` migration file simultaneously.

**Gateway routes**: When a new endpoint is added to any service that serves paths under `/api/v1/students/**`, you MUST add a specific route in `student-gateway/application.yml` BEFORE the `Path=/api/v1/students/**` catch-all. First-match-wins.

**CRITICAL — dual-gateway rule (Fix #127)**: nginx proxies ALL `/api/` traffic → `api-gateway` (8180). `student-gateway` (8089) is NEVER hit by browser traffic. Any new student-facing route MUST be added to BOTH `api-gateway/application.yml` AND `student-gateway/application.yml`. Adding to student-gateway alone = 404 in the browser.

**Flyway + DB ownership (this dev DB)**: Tables in `assess_db`, `auth_db`, etc. are owned by `srikanth` (superuser) because migrations ran as srikanth during initial setup. Future DDL migrations that use `ALTER TABLE` on these schemas must be run manually as `postgres`/`srikanth` superuser first, then inserted into `flyway_schema_history`. Fresh DBs are not affected (tables will be owned by the app user).

**start-all.sh find**: All `find` calls in `start_svc()` use `-maxdepth 1` — never remove this. WAR packaging creates exploded `WEB-INF/lib/` inside `target/` and without depth limiting, the first match is a dependency JAR, not the service artifact.

**Teacher dual-profile architecture**: TEACHER role has TWO backend profiles — (1) `mentor_schema.mentor_profiles` in mentor-svc (bio, specializations, hourlyRate, gender, district) accessed via `GET/PATCH /api/v1/mentors/me` with X-User-Id header injected by gateway; (2) `center_schema.teachers` in center-svc (phoneNumber, subjects, roleType, designation, qualification, yearsOfExperience, bio, district) — NO `/teachers/me` endpoint exists yet (pending Fix). Frontend MUST call `GET /api/v1/centers` → `centers[0]?.id` for teacher centerId — NEVER use `user?.centerId` (null in JWT until re-login after approval). `TeacherRepository.findByUserId(userId)` already exists — resolveAccessibleCenters() uses it.

**center-svc → auth-svc inter-service**: center-svc has NO REST client to call auth-svc. They communicate only via Kafka (`center-events` and `audit-immutable` topics). auth-svc does NOT consume center-events. If centerId sync after teacher approval is needed, it requires adding WebClient to center-svc pointing to AUTH_SVC_URI. Auth endpoint: `PATCH /api/v1/auth/admin/users/{userId}/center` (requires CENTER_ADMIN/INSTITUTION_ADMIN/SUPER_ADMIN role).

---

## AWS Deployment (EC2 + RDS — Tier 1) — LIVE at http://13.126.138.9

**Deployment scripts**: `data-backup/scripts/` (backup) + `data-backup/scripts/ec2-setup/` (EC2 setup)

### Deploying a New Feature to Live EC2

When you add a new feature, follow these steps to push it live:

#### Frontend-only change (fastest — 2 min)
```bash
# 1. Build
npx vite build   # Use npx vite build NOT npm run build (tsc fails on test file deps)

# 2. Fix nginx ownership + deploy
ssh -i ~/.ssh/edutech-key.pem -o KexAlgorithms=ecdh-sha2-nistp256 ec2-user@13.126.138.9 \
  "sudo chown -R ec2-user:ec2-user /usr/share/nginx/html"
scp -i ~/.ssh/edutech-key.pem -o KexAlgorithms=ecdh-sha2-nistp256 \
  -r frontend/web/dist/. ec2-user@13.126.138.9:/usr/share/nginx/html/
```

#### Java service change (5-15 min per service)
```bash
# 1. Build only the changed service (fast — skip unchanged modules)
mvn clean package -DskipTests -T 4 -Drevision=1.0.0-PROD \
  -pl services/<service-name> -am

# 2. Transfer WAR/JAR to EC2
# For Tomcat WARs (11 services):
SVC=auth-svc   # change this
scp -i ~/.ssh/edutech-key.pem -o KexAlgorithms=ecdh-sha2-nistp256 \
  services/$SVC/target/$SVC-1.0.0-PROD.war \
  ec2-user@13.126.138.9:/opt/apps/

# 3. Restart the service on EC2
ssh -i ~/.ssh/edutech-key.pem -o KexAlgorithms=ecdh-sha2-nistp256 ec2-user@13.126.138.9 "
  sudo systemctl stop edutech-$SVC
  rm -rf /opt/apps/tomcat-$SVC/webapps/ROOT /opt/apps/tomcat-$SVC/webapps/ROOT.war
  cp /opt/apps/$SVC-1.0.0-PROD.war /opt/apps/tomcat-$SVC/webapps/ROOT.war
  sudo systemctl start edutech-$SVC
  sleep 20
  curl -s http://localhost:<PORT>/actuator/health
"
```

#### Flyway DB migration (run before restarting the service)
- New migration file in `src/main/resources/db/migration/<schema>/V{N}__*.sql` is picked up automatically when the service starts (Flyway runs on boot).
- On RDS (EC2): no manual step needed — Flyway applies the migration on service restart.
- **Exception**: `ALTER TABLE` on existing tables in **local dev DB** (tables owned by `srikanth` superuser) — must run manually as superuser first.

#### EC2 SSH key
```bash
~/.ssh/edutech-key.pem    # always use: -o KexAlgorithms=ecdh-sha2-nistp256
EC2: 13.126.138.9  (i-0e9a180c6cb8af4c3, ap-south-1, t3.xlarge — Elastic IP, upgraded 2026-03-29)
```

**Build for production** (NEVER use -DskipITs — MfaServiceTest unit test fails; use -DskipTests):
```bash
mvn clean package -DskipTests -T 4 -Drevision=1.0.0-PROD
```

**Artifact breakdown** (16 services total):
- **12 Tomcat WARs**: auth-svc, parent-svc, center-svc, assess-svc, psych-svc, student-profile-svc, exam-tracker-svc, performance-svc, career-oracle-svc, mentor-svc, notification-svc, nexus-chat-svc
- **4 Exec (java -jar)**: ai-gateway-svc (exec WAR), api-gateway (JAR), student-gateway (JAR), **ai-mentor-svc (JAR — no WAR packaging)**
- **1 Python sidecar**: python-ai-svc (FastAPI/Uvicorn, port 8095)

**Local superuser for pg_dump**: `srikanth` (NOT `edutech_root` which doesn't exist). `.env` POSTGRES_ROOT_USER=srikanth, POSTGRES_ROOT_PASSWORD= (empty, peer auth).

**EC2 setup order** (first-time only): 01-java-kafka → 02-tomcat-instances (11 only) → 03-deploy-wars → 05-nginx → 06-systemd → 04-start-services

**Live URL:** http://13.126.138.9 (Elastic IP — permanent, won't change on restart)

**AWS state** → `memory/aws-deployment.md`

---

## Env File

`.env` in project root. Load: `while IFS='=' read -r key val; do [[ -z "$key" || "$key" == \#* ]] && continue; export "$key=$val"; done < .env`
- `AI_DEFAULT_PROVIDER=OPENROUTER` (arcee-ai/trinity-large-preview:free)
- `TWILIO_ACCOUNT_SID=dev_placeholder` → SMS logs only
- `CAPTCHA_E2E_BYPASS_TOKEN=E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD`
- `POSTGRES_ROOT_USER=srikanth` (local superuser for pg_dump — bypasses RLS + large objects)
