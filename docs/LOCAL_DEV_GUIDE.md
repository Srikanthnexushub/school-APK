# Local Development Guide

This guide walks through setting up the EduTech AI Platform on a local machine from a fresh clone to a fully running stack. Follow steps in order — each step depends on the previous one completing successfully.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [RSA Key Generation (JWT)](#rsa-key-generation-jwt)
4. [Starting Infrastructure](#starting-infrastructure)
5. [Database Initialization](#database-initialization)
6. [Building the Project](#building-the-project)
7. [Running Services](#running-services)
8. [Service Ports Reference](#service-ports-reference)
9. [Daily Session Startup Workflow](#daily-session-startup-workflow)
10. [Authentication & Login Reference](#authentication--login-reference)
11. [Verifying the Stack](#verifying-the-stack)
12. [Dev GUI Tools](#dev-gui-tools)
13. [Common Issues and Fixes](#common-issues-and-fixes)

---

## Prerequisites

| Tool | Minimum version | Notes |
|------|----------------|-------|
| Java JDK | 17 | Must be a JDK, not a JRE. [Eclipse Temurin](https://adoptium.net/) is recommended. Install via SDKMAN: `sdk install java 17.0.x-tem` |
| Maven | 3.9 | Verify: `mvn -version`. Ensure `JAVA_HOME` points to your JDK 17 installation. |
| Docker Desktop | 4.x | Compose V2 plugin is required (`docker compose` — no hyphen). Verify: `docker compose version` |
| Node.js | 18+ | Required for the frontend (`frontend/web/`). |
| Git | Any recent | |

Verify your setup before proceeding:

```bash
java -version          # expect: openjdk 17.x.x
mvn -version           # expect: Apache Maven 3.9.x, Java version: 17.x
docker compose version # expect: Docker Compose version v2.x.x
node -v                # expect: v18.x or v20.x
```

> **macOS note:** If you installed Java via Homebrew or Adoptium, add `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` to your shell profile so Maven consistently picks up JDK 17.

---

## Environment Setup

### 1. Clone the repository

```bash
git clone https://github.com/Srikanthnexushub/school-APK.git
cd school-APK
```

### 2. Create your local `.env` file

```bash
cp .env.example .env
```

`.env` is listed in `.gitignore` and is never committed. `.env.example` is the committed canonical reference — every variable is documented there with inline comments.

### 3. Fill in required values

Open `.env` in your editor. The table below identifies which variables are **required for local development** versus which can be deferred or left blank.

**PostgreSQL**

```dotenv
POSTGRES_HOST=localhost
POSTGRES_PORT=5433               # note: 5433 (not 5432) to avoid conflicts
POSTGRES_ROOT_USER=edutech_root
POSTGRES_ROOT_PASSWORD=EduTech_Dev_2026!

# Per-service credentials (one set per service — repeat pattern)
AUTH_SVC_DB_NAME=auth_db
AUTH_SVC_DB_USERNAME=auth_user
AUTH_SVC_DB_PASSWORD=auth_pass_dev

PARENT_SVC_DB_NAME=parent_db
PARENT_SVC_DB_USERNAME=parent_user
PARENT_SVC_DB_PASSWORD=parent_pass_dev

CENTER_SVC_DB_NAME=center_db
CENTER_SVC_DB_USERNAME=center_user
CENTER_SVC_DB_PASSWORD=center_pass_dev

ASSESS_SVC_DB_NAME=assess_db
ASSESS_SVC_DB_USERNAME=assess_user
ASSESS_SVC_DB_PASSWORD=assess_pass_dev

PSYCH_SVC_DB_NAME=psych_db
PSYCH_SVC_DB_USERNAME=psych_user
PSYCH_SVC_DB_PASSWORD=psych_pass_dev

AI_MENTOR_DB_NAME=ai_mentor_db
AI_MENTOR_DB_USER=ai_mentor_user
AI_MENTOR_DB_PASSWORD=ai_mentor_pass_dev

CAREER_ORACLE_DB_NAME=career_oracle_db
CAREER_ORACLE_DB_USER=career_oracle_user
CAREER_ORACLE_DB_PASSWORD=career_oracle_pass_dev

MENTOR_DB_NAME=mentor_db
MENTOR_DB_USER=mentor_user
MENTOR_DB_PASSWORD=mentor_pass_dev

STUDENT_PROFILE_DB_NAME=student_profile_db
STUDENT_PROFILE_DB_USER=student_profile_user
STUDENT_PROFILE_DB_PASSWORD=student_profile_pass_dev

EXAM_TRACKER_DB_NAME=exam_tracker_db
EXAM_TRACKER_DB_USER=exam_tracker_user
EXAM_TRACKER_DB_PASSWORD=exam_tracker_pass_dev

PERFORMANCE_DB_NAME=performance_db
PERFORMANCE_DB_USER=performance_user
PERFORMANCE_DB_PASSWORD=performance_pass_dev
```

**Redis**

```dotenv
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_dev_pass_2026   # must match Docker Redis startup config
REDIS_SSL_ENABLED=false              # MUST be false for local plaintext Redis
```

**Kafka (KRaft — no ZooKeeper)**

```dotenv
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_SCHEMA_REGISTRY_URL=http://localhost:8081
KAFKA_NODE_ID=1
KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
KAFKA_EXTERNAL_PORT=9092
KAFKA_AUTO_CREATE_TOPICS=true
KAFKA_DEFAULT_PARTITIONS=3
KAFKA_DEFAULT_REPLICATION_FACTOR=1
KAFKA_LOG_RETENTION_HOURS=168
```

**Kafka topic names** (required — no defaults in code):

```dotenv
KAFKA_TOPIC_AUTH_EVENTS=auth-events
KAFKA_TOPIC_AUDIT_IMMUTABLE=audit-immutable
KAFKA_TOPIC_NOTIFICATION_SEND=notification-send
KAFKA_TOPIC_PSYCH_EVENTS=psych-events
KAFKA_TOPIC_ASSESS_EVENTS=assess-events
KAFKA_TOPIC_CENTER_EVENTS=center-events
KAFKA_TOPIC_PARENT_EVENTS=parent-events
KAFKA_TOPIC_AI_MENTOR_STUDY_PLAN_CREATED=ai-mentor-study-plan-created
KAFKA_TOPIC_AI_MENTOR_DOUBT_SUBMITTED=ai-mentor-doubt-submitted
KAFKA_TOPIC_AI_MENTOR_DOUBT_RESOLVED=ai-mentor-doubt-resolved
KAFKA_TOPIC_CAREER_ORACLE_RECOMMENDED=career-oracle-recommended
KAFKA_TOPIC_MENTOR_SESSION_BOOKED=mentor-session-booked
KAFKA_TOPIC_MENTOR_SESSION_COMPLETED=mentor-session-completed
KAFKA_TOPIC_MENTOR_SESSION_FEEDBACK=mentor-session-feedback
KAFKA_TOPIC_AI_GATEWAY_EVENTS=ai-gateway-events
KAFKA_TOPIC_EXAM_EVENTS=exam-events
KAFKA_TOPIC_PERFORMANCE_EVENTS=performance-events
KAFKA_TOPIC_STUDENT_EVENTS=student-events
```

**JWT** — see [RSA Key Generation](#rsa-key-generation-jwt) below for key paths:

```dotenv
JWT_PRIVATE_KEY_PATH=/absolute/path/to/keys/jwt-private.pem
JWT_PUBLIC_KEY_PATH=/absolute/path/to/keys/jwt-public.pem
JWT_ACCESS_TOKEN_EXPIRY_SECONDS=900
JWT_REFRESH_TOKEN_EXPIRY_SECONDS=604800
JWT_ISSUER=https://edupath.local
JWT_JWKS_URI=http://localhost:8182/api/v1/auth/jwks
JWT_JWKS_CACHE_TTL_SECONDS=300
```

**Service ports** (required — no defaults in YAML for security-sensitive services):

```dotenv
API_GATEWAY_PORT=8180
AUTH_SVC_PORT=8182
PARENT_SVC_PORT=8082
CENTER_SVC_PORT=8083
ASSESS_SVC_PORT=8084
PSYCH_SVC_PORT=8085
AI_GATEWAY_SVC_PORT=8086
CAREER_ORACLE_SVC_PORT=8087
MENTOR_SVC_PORT=8088
STUDENT_GATEWAY_PORT=8089
STUDENT_PROFILE_SVC_PORT=8090
EXAM_TRACKER_SVC_PORT=8091
PERFORMANCE_SVC_PORT=8092
AI_MENTOR_SVC_PORT=8093
NOTIFICATION_SVC_PORT=8094
PYTHON_AI_SVC_PORT=8095
```

**Downstream URIs** (api-gateway routing):

```dotenv
AUTH_SVC_URI=http://localhost:8182
PARENT_SVC_URI=http://localhost:8082
CENTER_SVC_URI=http://localhost:8083
ASSESS_SVC_URI=http://localhost:8084
PSYCH_SVC_URI=http://localhost:8085
AI_GATEWAY_SVC_URI=http://localhost:8086
AI_GATEWAY_BASE_URL=http://localhost:8086
```

**Downstream URIs** (student-gateway routing):

```dotenv
STUDENT_PROFILE_SVC_URI=http://localhost:8090
EXAM_TRACKER_SVC_URI=http://localhost:8091
PERFORMANCE_SVC_URI=http://localhost:8092
AI_MENTOR_SVC_URI=http://localhost:8093
CAREER_ORACLE_SVC_URI=http://localhost:8087
MENTOR_SVC_URI=http://localhost:8088
NOTIFICATION_SVC_URI=http://localhost:8094
```

**AI / LLM** (optional — services degrade gracefully without a real key):

```dotenv
ANTHROPIC_API_KEY=sk-ant-dev-placeholder   # leave as-is for local dev; triggers local echo mode
ANTHROPIC_MODEL=claude-sonnet-4-6
ANTHROPIC_BASE_URL=https://api.anthropic.com
OPENAI_API_KEY=sk-dev-placeholder
OPENROUTER_API_KEY=or-dev-placeholder      # leave as-is for local dev; triggers local echo mode
OPENROUTER_BASE_URL=https://openrouter.ai
OPENROUTER_MODEL=openai/gpt-4o-mini
OPENROUTER_CONNECT_TIMEOUT_MS=5000
OPENROUTER_READ_TIMEOUT_MS=30000
OLLAMA_BASE_URL=http://localhost:11434
```

> **Local AI without a real API key:** When `ANTHROPIC_API_KEY` starts with `sk-ant-dev` (or is blank), or `OPENROUTER_API_KEY` starts with `or-dev` (or is blank), `ai-gateway-svc` automatically activates **local-echo mode** — a rule-based fallback that returns contextually appropriate responses based on keywords in the system/user prompt. The Parent Copilot, Student AI Mentor, and all other AI features remain fully usable without any external API subscription.

**hCaptcha** (use Google's official test keys for local dev):

```dotenv
HCAPTCHA_VERIFY_URL=https://www.google.com/recaptcha/api/siteverify
HCAPTCHA_SITE_KEY=6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI
HCAPTCHA_SECRET_KEY=6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe
```

**Internal service-to-service key:**

```dotenv
SERVICE_API_KEY=edutech-internal-svc-2026
```

**Argon2id** (OWASP 2024 recommended):

```dotenv
ARGON2_MEMORY_COST=65536
ARGON2_ITERATIONS=3
ARGON2_PARALLELISM=4
ARGON2_SALT_LENGTH=16
ARGON2_HASH_LENGTH=32
```

**OTP:**

```dotenv
OTP_EXPIRY_SECONDS=300
OTP_MAX_ATTEMPTS=5
OTP_LENGTH=6
```

**Observability:**

```dotenv
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
ACTUATOR_ENDPOINTS=health,info,metrics
APP_ENVIRONMENT=local
OTEL_SAMPLING_PROBABILITY=1.0
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
```

**Gateway rate limiters:**

```dotenv
GATEWAY_RATE_LIMIT_REPLENISH_RATE=20
GATEWAY_RATE_LIMIT_BURST_CAPACITY=40
GATEWAY_RATE_LIMIT_REQUESTED_TOKENS=1
STUDENT_GW_RATE_LIMIT_REPLENISH_RATE=10
STUDENT_GW_RATE_LIMIT_BURST_CAPACITY=20
STUDENT_GW_RATE_LIMIT_REQUESTED_TOKENS=1
```

**Dev GUI tool ports:**

```dotenv
KAFKA_UI_CLUSTER_NAME=edutech-local
KAFKA_UI_PORT=9080
PGADMIN_EMAIL=admin@edupath.local
PGADMIN_PASSWORD=pgadmin_dev_pass
PGADMIN_PORT=5050
REDIS_COMMANDER_PORT=8087
```

**Variables that can be deferred** (leave blank or as placeholder unless testing the specific feature):

| Variable | Feature gated |
|----------|--------------|
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_VERIFY_SERVICE_SID` | SMS OTP in `auth-svc` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Email OTP in `auth-svc` |
| `FIREBASE_PROJECT_ID`, `FIREBASE_SERVICE_ACCOUNT_KEY_PATH` | Push notifications |
| `VAULT_URI`, `VAULT_TOKEN`, `VAULT_NAMESPACE` | HashiCorp Vault — production only |
| CAT engine vars (`CAT_MIN_QUESTIONS`, etc.) | Computer Adaptive Testing in `assess-svc` |
| STOMP relay vars (`ASSESS_SVC_WS_*`) | WebSocket exam sessions in `assess-svc` |

---

## RSA Key Generation (JWT)

The platform uses **RS256 asymmetric signing**. `auth-svc` signs JWTs with an RSA-2048 private key; all other services verify tokens using the public key only. No shared secrets are distributed.

Generate a key pair with OpenSSL (pre-installed on macOS; available via `brew install openssl` or your system package manager):

```bash
# Store keys outside the repository — never commit private keys
mkdir -p ~/IdeaProjects/school-APK/keys

# Generate RSA-2048 private key
openssl genrsa -out ~/IdeaProjects/school-APK/keys/jwt-private.pem 2048

# Extract the public key
openssl rsa -in ~/IdeaProjects/school-APK/keys/jwt-private.pem -pubout \
  -out ~/IdeaProjects/school-APK/keys/jwt-public.pem
```

Set the absolute paths in `.env`:

```dotenv
JWT_PRIVATE_KEY_PATH=/Users/your-username/IdeaProjects/school-APK/keys/jwt-private.pem
JWT_PUBLIC_KEY_PATH=/Users/your-username/IdeaProjects/school-APK/keys/jwt-public.pem
```

Verify the generated files look correct:

```bash
head -1 ~/IdeaProjects/school-APK/keys/jwt-private.pem  # -----BEGIN RSA PRIVATE KEY-----
head -1 ~/IdeaProjects/school-APK/keys/jwt-public.pem   # -----BEGIN PUBLIC KEY-----
```

> **Security:** `keys/` is in `.gitignore`. Regenerating keys invalidates all previously issued tokens — all logged-in users will need to log in again.

---

## Starting Infrastructure

All local infrastructure is defined in `infrastructure/docker/docker-compose.yml`. It reads values from `.env` via the `--env-file` flag.

```bash
cd infrastructure/docker

# Start all containers in detached mode
docker compose --env-file ../../.env up -d

# Verify containers are healthy (wait ~60 seconds after startup)
docker compose --env-file ../../.env ps

# Return to project root
cd ../..
```

**What starts:**

| Container | Image | Default port | Purpose |
|-----------|-------|-------------|---------|
| `edutech-postgres` | `timescale/timescaledb-ha:pg16-latest` | `5433` | PostgreSQL 16 with pgvector and TimescaleDB extensions built in |
| `edutech-redis` | `redis:7-alpine` | `6379` | Session store, JWT token blacklist, rate limiter backend, OTP storage |
| `edutech-kafka` | `bitnami/kafka:3.6` | `9092` | Event bus — KRaft mode, no ZooKeeper required |
| `edutech-kafka-ui` | `provectuslabs/kafka-ui:latest` | `$KAFKA_UI_PORT` | Browser Kafka management (dev only) |
| `edutech-pgadmin` | `dpage/pgadmin4:latest` | `$PGADMIN_PORT` | PostgreSQL GUI (dev only) |
| `edutech-redis-commander` | `rediscommander/redis-commander:latest` | `$REDIS_COMMANDER_PORT` | Redis key browser (dev only) |

Kafka runs in **KRaft mode** — a single container acts as both controller and broker. `KAFKA_AUTO_CREATE_TOPICS=true` means topic creation is handled automatically on first publish.

**Teardown** (keeps volumes — data is preserved on next `up -d`):

```bash
docker compose --env-file ../../.env down
```

**Full reset** (deletes all persisted data — use when you need a clean slate):

```bash
docker compose --env-file ../../.env down -v
```

---

## Database Initialization

**Flyway runs automatically** on each service startup. There is no manual SQL to execute.

Each service:

1. Connects to its own named database (e.g., `auth_db`, `center_db`).
2. Creates its dedicated schema (e.g., `auth_schema`, `center_schema`) if it does not exist.
3. Runs all migration scripts from `services/{svc}/src/main/resources/db/migration/`.
4. Records applied migrations in `flyway_schema_history` within the schema.

The `timescale/timescaledb-ha:pg16-latest` image bundles both `pgvector` and `TimescaleDB` — no manual extension installation is required.

To reinitialize a service schema (e.g., after a destructive schema change):

```bash
# Drop the schema via psql or pgAdmin
DROP SCHEMA auth_schema CASCADE;
# Then restart auth-svc — Flyway recreates and repopulates it
```

---

## Building the Project

The Maven multi-module build resolves modules in the order declared in the root `pom.xml`. Shared libraries are always built before services.

```bash
# Load environment (required before running any service)
export $(grep -v '^#' .env | grep -v '^ *$' | xargs)

# Compile all modules (no tests)
mvn compile --no-transfer-progress

# Run all 219 tests (Testcontainers / WireMock — no manual infra required)
mvn test --no-transfer-progress

# Test a single service — --also-make builds library dependencies first
mvn test -pl services/auth-svc --also-make --no-transfer-progress
mvn test -pl services/assess-svc --also-make --no-transfer-progress

# Package a runnable JAR for a single service
mvn package -pl services/auth-svc --also-make -DskipTests --no-transfer-progress
# Output: services/auth-svc/target/auth-svc-1.0.0-SNAPSHOT.jar

# Override project version (used in CI)
mvn compile -Drevision=1.2.0
```

> **IntelliJ IDEA:** Import the root `pom.xml` as a Maven project. All modules are discovered automatically. Create Spring Boot run configurations per service; set environment variables by clicking "Modify options → Load from .env file" in the run configuration.

---

## Running Services

Each service reads its configuration entirely from environment variables — no hardcoded values exist in the codebase. Load `.env` into your shell before starting any service.

### Load environment variables (do this once per terminal session)

```bash
# From the project root
export $(grep -v '^#' .env | grep -v '^ *$' | xargs)
```

### Option A: Maven Spring Boot plugin (development)

```bash
# Open a separate terminal per service (or use IntelliJ run dashboard)
mvn -pl services/auth-svc spring-boot:run --no-transfer-progress
```

### Option B: Packaged JAR

```bash
# Build first
mvn package -pl services/auth-svc --also-make -DskipTests --no-transfer-progress

# Then run
java -jar services/auth-svc/target/auth-svc-1.0.0-SNAPSHOT.jar
```

### Recommended startup order

Start services in this order to avoid transient connection errors on boot:

1. **Infrastructure** (Docker): confirm all containers are `(healthy)`.
2. **`auth-svc`** (port 8182) — provides JWT signing; JWKS endpoint must be up before other services validate tokens.
3. **Core domain services** (any order): `center-svc` (8083), `parent-svc` (8082), `assess-svc` (8084), `psych-svc` (8085).
4. **EduPath student services** (any order): `student-profile-svc` (8090), `exam-tracker-svc` (8091), `performance-svc` (8092), `ai-mentor-svc` (8093), `career-oracle-svc` (8087), `mentor-svc` (8088).
5. **AI gateway** — `ai-gateway-svc` (8086): works in local-echo mode without a real API key.
6. **Notification** — `notification-svc` (8094): Kafka consumer for notification events.
7. **Gateways last**: `api-gateway` (8180) and `student-gateway` (8089) — start after all downstream services.

> **Frontend:** After all backend services are running, start the Vite dev server from `frontend/web/`:
> ```bash
> cd frontend/web && npm install && npm run dev
> ```
> The Vite proxy routes `/api/v1/students/**`, `/api/v1/exam-tracker/**`, `/api/v1/performance/**`, `/api/v1/doubts/**`, `/api/v1/recommendations/**`, `/api/v1/study-plans/**`, `/api/v1/career-profiles/**`, `/api/v1/mentor-sessions/**`, `/api/v1/mentors/**` to **student-gateway (8089)** and all other `/api/**` paths to **api-gateway (8180)**.

---

## Service Ports Reference

| Service | Port | Gateway | Route prefix |
|---------|------|---------|--------------|
| `api-gateway` | **8180** | — | entry point for all non-student routes |
| `auth-svc` | **8182** | api-gateway | `/api/v1/auth/**`, `/api/v1/otp/**` |
| `parent-svc` | 8082 | api-gateway | `/api/v1/parents/**`, `/api/v1/copilot/**` |
| `center-svc` | 8083 | api-gateway | `/api/v1/centers/**` |
| `assess-svc` | 8084 | api-gateway | `/api/v1/exams/**`, `/api/v1/questions/**` |
| `psych-svc` | 8085 | api-gateway | `/api/v1/psych/**` |
| `ai-gateway-svc` | 8086 | api-gateway | `/api/v1/ai/**` |
| `career-oracle-svc` | 8087 | student-gateway | `/api/v1/career-profiles/**`, `/api/v1/career-recommendations/**`, `/api/v1/college-predictions/**` |
| `mentor-svc` | 8088 | student-gateway | `/api/v1/mentors/**`, `/api/v1/mentor-sessions/**` |
| `student-gateway` | **8089** | — | entry point for all EduPath student routes |
| `student-profile-svc` | 8090 | student-gateway | `/api/v1/students/**` |
| `exam-tracker-svc` | 8091 | student-gateway | `/api/v1/exam-tracker/**` |
| `performance-svc` | 8092 | student-gateway | `/api/v1/performance/**` |
| `ai-mentor-svc` | 8093 | student-gateway | `/api/v1/doubts/**`, `/api/v1/recommendations/**`, `/api/v1/study-plans/**` |
| `notification-svc` | 8094 | api-gateway | `/api/v1/notifications/**` |
| Python AI sidecar | 8095 | internal | called by `ai-gateway-svc` and `psych-svc` only |

> **gRPC ports:** assess-svc=9093, performance-svc=9094, ai-mentor-svc=9096, career-oracle-svc=9097, mentor-svc=9098

---

## Daily Session Startup Workflow

Every new terminal session requires re-loading the environment. JWTs expire after 900 seconds (15 minutes) — you will need to log in again at the start of each dev session.

### Step 1: Start infrastructure

```bash
cd /path/to/school-APK/infrastructure/docker
docker compose --env-file ../../.env up -d
cd ../..
```

### Step 2: Load env vars

```bash
# Do this in EVERY terminal that will run a service
export $(grep -v '^#' .env | grep -v '^ *$' | xargs)
```

### Step 3: Start services (in new terminals or background)

```bash
# Terminal per service, or use IntelliJ run dashboard
mvn -pl services/auth-svc spring-boot:run --no-transfer-progress
mvn -pl services/assess-svc spring-boot:run --no-transfer-progress
# ... etc.
```

### Step 4: Verify services are up

```bash
curl -s http://localhost:8180/actuator/health | jq .status  # api-gateway
curl -s http://localhost:8182/actuator/health | jq .status  # auth-svc
curl -s http://localhost:8089/actuator/health | jq .status  # student-gateway
```

### Step 5: Log in to obtain a fresh JWT

JWTs expire after **15 minutes** (`JWT_ACCESS_TOKEN_EXPIRY_SECONDS=900`). You must log in at the start of each session and whenever you get a 401 response.

See [Authentication & Login Reference](#authentication--login-reference) below.

---

## Authentication & Login Reference

### Login request format

The login endpoint at `POST http://localhost:8180/api/v1/auth/login` requires:

```json
{
  "email": "your-email@example.com",
  "password": "YourPassword123!",
  "captchaToken": "{challengeId}:{answer}",
  "deviceFingerprint": {
    "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
    "deviceId": "dev-machine-01",
    "ipSubnet": "127.0.0"
  }
}
```

**Critical notes:**
- `captchaToken` must be in the format `{challengeId}:{answer}` — not a raw captcha string.
- `deviceFingerprint` is a **JSON object** with three fields — NOT a plain string or hash.
- The `deviceFingerprint` must match on every login for the same device. Using an inconsistent `deviceId` triggers a new device challenge.

### Getting a captcha token (local dev bypass)

```bash
# 1. Request a new captcha challenge
CHALLENGE=$(curl -s http://localhost:8180/api/v1/otp/captcha | jq -r '.challengeId')

# 2. Get the answer from Redis (password: redis_dev_pass_2026)
ANSWER=$(docker exec edutech-redis redis-cli -a redis_dev_pass_2026 GET "captcha:${CHALLENGE}" 2>/dev/null)

# 3. Compose the token
echo "captchaToken = ${CHALLENGE}:${ANSWER}"
```

### Full login example (curl)

```bash
# Step 1: get captcha
RESP=$(curl -s http://localhost:8180/api/v1/otp/captcha)
CHALLENGE=$(echo $RESP | jq -r '.challengeId')
ANSWER=$(docker exec edutech-redis redis-cli -a redis_dev_pass_2026 GET "captcha:${CHALLENGE}" 2>/dev/null)

# Step 2: login
TOKEN_RESP=$(curl -s -X POST http://localhost:8180/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"admin1@test.com\",
    \"password\": \"Admin@2026!\",
    \"captchaToken\": \"${CHALLENGE}:${ANSWER}\",
    \"deviceFingerprint\": {
      \"userAgent\": \"curl/local-dev\",
      \"deviceId\": \"dev-machine-01\",
      \"ipSubnet\": \"127.0.0\"
    }
  }")

TOKEN=$(echo $TOKEN_RESP | jq -r '.accessToken')
echo "Bearer $TOKEN"
```

### Token expiry handling

- Access token expires in **15 minutes** (900 seconds). After expiry you will receive `401 Unauthorized`.
- Refresh token expires in **7 days** (604800 seconds).
- Use `POST /api/v1/auth/refresh` with the refresh token to obtain a new access token without re-doing captcha.
- If both tokens have expired, repeat the full login flow above.

### Injecting token into frontend (for E2E testing)

The frontend uses Zustand with localStorage persistence under the key `edupath-auth`. To bypass the login UI:

```javascript
// Open browser DevTools → Console → paste:
localStorage.setItem('edupath-auth', JSON.stringify({
  state: {
    token: '<your-access-token>',
    user: { id: '<userId>', email: '<email>', role: '<STUDENT|TEACHER|ADMIN|PARENT>' },
    isAuthenticated: true
  },
  version: 0
}));
location.reload();
```

### Test accounts (local dev seed data)

| Role | Email | Notes |
|------|-------|-------|
| ADMIN (CENTER_ADMIN) | `admin1@test.com` | Center admin — access to all center management routes |
| TEACHER | `teacher1@test.com` | Teacher role — exam creation wizard, AI insights |
| STUDENT | `qa-test@nexused.dev` | Student ID: `ea008897-3fa1-44e4-88bd-3ce8c7df656c` |
| PARENT | Create via registration | Link to student after creation |

To reset a password (Argon2id hash update in `auth_db`):

```bash
# Generate hash with correct parameters (p=4 is CRITICAL)
# Then update in DB:
docker exec -it edutech-postgres psql -U edutech_root -d auth_db \
  -c "UPDATE auth_schema.credentials SET password_hash = '<new-hash>' WHERE email = 'your@email.com';"
```

---

## Verifying the Stack

Once infrastructure is healthy and services are running:

### Health endpoints

```bash
# Gateways
curl -s http://localhost:8180/actuator/health | jq .status   # api-gateway
curl -s http://localhost:8089/actuator/health | jq .status   # student-gateway

# Core services (via api-gateway)
curl -s http://localhost:8182/actuator/health | jq .status   # auth-svc
curl -s http://localhost:8082/actuator/health | jq .status   # parent-svc
curl -s http://localhost:8083/actuator/health | jq .status   # center-svc
curl -s http://localhost:8084/actuator/health | jq .status   # assess-svc
curl -s http://localhost:8085/actuator/health | jq .status   # psych-svc
curl -s http://localhost:8086/actuator/health | jq .status   # ai-gateway-svc

# EduPath services (via student-gateway)
curl -s http://localhost:8090/actuator/health | jq .status   # student-profile-svc
curl -s http://localhost:8091/actuator/health | jq .status   # exam-tracker-svc
curl -s http://localhost:8092/actuator/health | jq .status   # performance-svc
curl -s http://localhost:8093/actuator/health | jq .status   # ai-mentor-svc
curl -s http://localhost:8087/actuator/health | jq .status   # career-oracle-svc
curl -s http://localhost:8088/actuator/health | jq .status   # mentor-svc
curl -s http://localhost:8094/actuator/health | jq .status   # notification-svc
```

A fully healthy response:

```json
{ "status": "UP" }
```

### Swagger UI

The `api-gateway` aggregates OpenAPI specs from all running downstream services into a single Swagger UI.

```
http://localhost:8180/swagger-ui.html        # aggregated — all services (api-gateway routes)
http://localhost:8182/swagger-ui.html        # auth-svc standalone
http://localhost:8082/swagger-ui.html        # parent-svc standalone
http://localhost:8083/swagger-ui.html        # center-svc standalone
http://localhost:8084/swagger-ui.html        # assess-svc standalone
http://localhost:8085/swagger-ui.html        # psych-svc standalone
http://localhost:8086/swagger-ui.html        # ai-gateway-svc standalone
```

Raw OpenAPI JSON for each service is available at `http://localhost:{port}/api-docs`.

### Frontend

```
http://localhost:5173/          # Vite dev server (student/parent/teacher login)
http://localhost:5173/login     # Login page
```

---

## Dev GUI Tools

| Tool | URL | Purpose |
|------|-----|---------|
| Kafka UI | `http://localhost:9080` | Browse topics, consumer group offsets, message payloads |
| pgAdmin | `http://localhost:5050` | PostgreSQL GUI — log in with `PGADMIN_EMAIL` / `PGADMIN_PASSWORD` |
| Redis Commander | `http://localhost:8087` | Browse Redis keys, inspect token blacklist and OTP state |

**Connecting pgAdmin to local PostgreSQL:**

1. Open `http://localhost:5050` and log in.
2. Right-click **Servers** > **Register** > **Server**.
3. **General** tab: Name = `EduTech Local`
4. **Connection** tab: Host = `host.docker.internal` (macOS/Windows) or Docker bridge IP (Linux), Port = `5433`, Username = `edutech_root`, Password = `EduTech_Dev_2026!`.

**Inspecting Redis OTP/captcha keys:**

```bash
# List all keys matching a pattern
docker exec edutech-redis redis-cli -a redis_dev_pass_2026 KEYS "captcha:*"

# Get a specific captcha answer
docker exec edutech-redis redis-cli -a redis_dev_pass_2026 GET "captcha:<challengeId>"

# List active refresh tokens for a user
docker exec edutech-redis redis-cli -a redis_dev_pass_2026 SMEMBERS "rt:user:<userId>"
```

---

## Common Issues and Fixes

### Port conflict on startup

**Symptom:** `Address already in use: bind` in Docker or service logs.

**Fix:** Find the process occupying the port and stop it, or change the port assignment in `.env`.

```bash
lsof -i :8180    # find what is using port 8180
kill -9 <PID>
```

---

### Redis connection refused

**Symptom:** `io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379`

**Causes and fixes:**

1. Redis container is not running — check `docker compose ps`.
2. `REDIS_SSL_ENABLED=true` is set locally — the local Redis container runs without TLS. Set `REDIS_SSL_ENABLED=false` in `.env`.
3. `REDIS_PASSWORD` mismatch — the password in `.env` must match the one used when the container started. If you changed it after the container was created, run `docker compose down -v && docker compose up -d`.

---

### Kafka timeout at service startup

**Symptom:** `org.apache.kafka.common.errors.TimeoutException: Topic X not present in metadata`

**Cause:** The Kafka KRaft broker needs 30–60 seconds to complete leader election after the container starts.

**Fix:** Wait for Kafka to report `healthy` before starting services:

```bash
docker compose --env-file ../../.env ps
# Wait until edutech-kafka shows: (healthy)
```

Manually confirm Kafka is ready:

```bash
docker exec edutech-kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

---

### JWT validation fails — 401 Unauthorized

**Symptom:** All API requests return `401 Unauthorized` after services have been running for a while.

**Cause:** JWT access tokens expire after 15 minutes (`JWT_ACCESS_TOKEN_EXPIRY_SECONDS=900`). This is expected behaviour.

**Fix:** Log in again to obtain a fresh token. See [Authentication & Login Reference](#authentication--login-reference).

---

### JWT validation fails — signature mismatch

**Symptom:** `io.jsonwebtoken.security.SignatureException: JWT signature does not match locally computed signature`

**Causes and fixes:**

1. `JWT_PUBLIC_KEY_PATH` on the validating service points to a different key than `JWT_PRIVATE_KEY_PATH` on `auth-svc`. Both must reference the same RSA key pair.
2. The PEM file path is wrong or the file is not readable — verify: `cat $JWT_PUBLIC_KEY_PATH`.
3. Keys were regenerated after tokens were issued — all existing tokens are immediately invalid. Log in again.

---

### Login returns 400 — captchaToken or deviceFingerprint format error

**Symptom:** `POST /api/v1/auth/login` returns HTTP 400.

**Most common causes:**

1. **`deviceFingerprint` is a string instead of an object.** It must be a JSON object with three fields:
   ```json
   {
     "userAgent": "...",
     "deviceId": "...",
     "ipSubnet": "..."
   }
   ```
   Sending a SHA-256 hex string or any other format will cause 400/500.

2. **`captchaToken` format is wrong.** Must be `{challengeId}:{answer}` — a colon-delimited string composed of the UUID from `/api/v1/otp/captcha` and the answer read from Redis.

3. **Wrong field name.** The field is `captchaToken` (not `captchaResponse`, not `token`).

---

### Login returns 500 — unexpected server error

**Symptom:** `POST /api/v1/auth/login` returns HTTP 500.

**Cause:** Usually means `deviceFingerprint` was deserialized into a Java `String` instead of a `DeviceFingerprint` record. This happens when the JSON body sends `"deviceFingerprint": "some-string"` instead of an object.

**Fix:** Send `deviceFingerprint` as a JSON object (see above).

---

### Parent Copilot shows "unable to connect to AI assistant"

**Cause:** `ANTHROPIC_API_KEY` is not set, is blank, or is `sk-ant-dev-placeholder` AND the local-echo fallback is not active.

**Expected behaviour:** When `ANTHROPIC_API_KEY` starts with `sk-ant-dev` or is blank, `ai-gateway-svc` automatically uses **local-echo mode** — no external API call is made. If the error still appears:

1. Confirm `ai-gateway-svc` is running: `curl -s http://localhost:8086/actuator/health | jq .status`
2. Confirm the api-gateway routes `/api/v1/ai/**` to port 8086.
3. Confirm `parent-svc` can reach ai-gateway-svc on port 8086 (service-to-service call, not via gateway).
4. Check `ai-gateway-svc` logs for `AiProviderException` — if the key is being sent to Anthropic despite being a placeholder, rebuild and restart `ai-gateway-svc`.

---

### `Could not resolve placeholder` error at startup

**Symptom:** `java.lang.IllegalArgumentException: Could not resolve placeholder '${SOME_VAR}' in value "${SOME_VAR}"`

**Cause:** A required environment variable is missing or blank in `.env`.

**Fix:** Find the variable in `.env.example`, add it to `.env` with an appropriate local value, then restart the service. The most common missed variables are the newly added EduPath service ports (`STUDENT_GATEWAY_PORT`, `NOTIFICATION_SVC_PORT`, etc.).

**IMPORTANT:** Always run `export $(grep -v '^#' .env | grep -v '^ *$' | xargs)` before starting services — values in `.env` are NOT automatically available in the shell without this command (unlike Docker Compose, which reads them automatically).

---

### Flyway migration error — non-empty schema without history table

**Symptom:** `FlywayException: Found non-empty schema(s) "auth_schema" without schema history table!`

**Cause:** Tables were manually created in the schema before Flyway ran for the first time.

**Fix:** Drop the schema and let Flyway recreate it cleanly:

```bash
# Via psql or pgAdmin:
DROP SCHEMA auth_schema CASCADE;
# Restart the service — Flyway recreates and migrates the schema.
```

Alternatively, do a full infrastructure reset:

```bash
cd infrastructure/docker
docker compose --env-file ../../.env down -v
docker compose --env-file ../../.env up -d
```

---

### pgvector extension not found

**Symptom:** Flyway migration fails with `ERROR: extension "vector" does not exist`

**Cause:** A non-pgvector-enabled Postgres image is in use, or the container was replaced.

**Fix:** Confirm `infrastructure/docker/docker-compose.yml` uses `timescale/timescaledb-ha:pg16-latest`. If needed, install manually:

```bash
docker exec -it edutech-postgres psql -U edutech_root \
  -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

---

### `JAVA_HOME` not set correctly — wrong JDK used

**Symptom:** Maven compiles with an unexpected JDK version, or `java -version` shows a different version than `mvn -version`.

**Fix:**

```bash
# macOS with Temurin 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

java -version    # must show 17.x
mvn -version     # must show Java version: 17.x
```

Add these exports to `~/.zshrc` or `~/.bashrc` to persist across terminal sessions.

---

### Maven build fails with `${revision}` not resolved

**Symptom:** `Could not resolve placeholder 'revision'` or artifact version shows `${revision}` literally.

**Cause:** Building a service module directly without `--also-make` skips the parent POM resolution.

**Fix:** Always use `--also-make` (`-am`) when building or testing a single module:

```bash
# Correct
mvn package -pl services/auth-svc --also-make -DskipTests

# Wrong — missing --also-make
mvn package -pl services/auth-svc -DskipTests
```

---

### Avatar crash — `Cannot read properties of undefined (reading 'split')`

**Symptom:** Browser console shows `TypeError: Cannot read properties of undefined (reading 'split')` from `Avatar.tsx`.

**Cause:** The user object injected from the JWT may contain `email` and `role` but no `name` field. The `Avatar` component must handle `undefined` gracefully.

**Status:** Fixed in `frontend/web/src/components/ui/Avatar.tsx` — both `getGradient` and `getInitials` use `const safe = name || '?'` as a null guard.

---

### assess-svc returns 403 — Hibernate treats entity as detached

**Symptom:** Creating exams, questions, submissions, or enrollments returns 403 (or silent 500 in logs shows `DataIntegrityViolationException`).

**Cause:** Domain model factory methods that call `UUID.randomUUID()` AND also have `@GeneratedValue` — Hibernate treats them as detached (non-null ID = update, not insert).

**Status:** Fixed — all 6 assess-svc domain models (Submission, Grade, Exam, ExamEnrollment, Question, SubmissionAnswer) have manual UUID assignment removed from factory methods.

---

### Student gets 403 on published exam

**Symptom:** Student receives 403 when fetching exam details or listing questions for a PUBLISHED exam.

**Status:** Fixed — `ExamService.getExam()` and `QuestionService.listQuestions()` allow STUDENT role on PUBLISHED exams. The `belongsToCenter` check is only applied to TEACHER/ADMIN roles.
