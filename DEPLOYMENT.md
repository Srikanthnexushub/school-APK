# Secure Deployment Guide
## EduTech AI Platform — school-APK

> **Repository:** https://github.com/Srikanthnexushub/school-APK
> **Prepared for:** Development Team / Subordinate Handoff
> **Classification:** Internal — Do NOT commit secrets
> **Date:** 2026-03-24

---

## Quick Links

| Resource | URL |
|---|---|
| **Source Code** | https://github.com/Srikanthnexushub/school-APK |
| **Clone (HTTPS)** | `git clone https://github.com/Srikanthnexushub/school-APK.git` |
| **Clone (SSH)** | `git clone git@github.com:Srikanthnexushub/school-APK.git` |
| **Frontend (dev)** | http://localhost:3000 |
| **API Gateway (dev)** | http://localhost:8180 |
| **Student Gateway (dev)** | http://localhost:8089 |

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [First-Time Local Setup](#2-first-time-local-setup)
3. [Daily Development Workflow](#3-daily-development-workflow)
4. [Service Architecture & Ports](#4-service-architecture--ports)
5. [Security Configuration](#5-security-configuration)
6. [Production Deployment Checklist](#6-production-deployment-checklist)
7. [Secrets Management](#7-secrets-management)
8. [Common Issues & Fixes](#8-common-issues--fixes)
9. [Team Access & Branch Strategy](#9-team-access--branch-strategy)

---

## 1. Prerequisites

### Required Software

| Software | Version | Notes |
|---|---|---|
| **Java (JDK)** | 17 (Microsoft OpenJDK recommended) | `JAVA_HOME` must be set |
| **Maven** | 3.9+ | Run from project root ONLY |
| **Node.js** | 18+ | For frontend |
| **Docker Desktop** | Latest | For Redis/Kafka/MailHog/MinIO |
| **PostgreSQL** | 16 (via Homebrew) | LOCAL — NOT Docker |
| **Git** | 2.x+ | |

### macOS Installation

```bash
# Install Homebrew (if not installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java 17
brew install --cask microsoft-openjdk17

# Install PostgreSQL 16
brew install postgresql@16
echo 'export PATH="/usr/local/opt/postgresql@16/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# Install Maven
brew install maven

# Install Node.js
brew install node@18

# Install Docker Desktop
# Download from: https://www.docker.com/products/docker-desktop/
```

### Linux (Ubuntu/Debian)

```bash
# Java 17
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk

# Maven
sudo apt-get install -y maven

# Node.js 18
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# PostgreSQL 16
sudo sh -c 'echo "deb https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -
sudo apt-get update
sudo apt-get install -y postgresql-16

# Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

---

## 2. First-Time Local Setup

### Step 1: Clone the Repository

```bash
git clone https://github.com/Srikanthnexushub/school-APK.git
cd school-APK
```

### Step 2: Configure Environment

```bash
# Copy example env file (⛔ NEVER commit .env)
cp .env.example .env
```

Open `.env` and configure these REQUIRED values:

```bash
# ── PostgreSQL ──────────────────────────────────────────
POSTGRES_ROOT_USER=srikanth          # your macOS username
POSTGRES_ROOT_PASSWORD=              # leave blank for peer auth
POSTGRES_HOST=localhost
POSTGRES_PORT=5432

# ── JWT (RSA keys — generate once) ──────────────────────
JWT_PRIVATE_KEY_PATH=/path/to/project/keys/jwt-private.pem
JWT_PUBLIC_KEY_PATH=/path/to/project/keys/jwt-public.pem
JWT_ACCESS_TOKEN_EXPIRY_SECONDS=7200   # 2 hours
JWT_REFRESH_TOKEN_EXPIRY_SECONDS=604800 # 7 days

# ── Redis ────────────────────────────────────────────────
REDIS_PASSWORD=your_redis_password_here

# ── MinIO (S3) ───────────────────────────────────────────
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin123       # CHANGE IN PRODUCTION
MINIO_ENDPOINT=http://localhost:9002

# ── AI Provider ──────────────────────────────────────────
AI_DEFAULT_PROVIDER=OPENROUTER
OPENROUTER_API_KEY=your_openrouter_key_here

# ── E2E Testing Only ─────────────────────────────────────
CAPTCHA_E2E_BYPASS_TOKEN=E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD
```

### Step 3: Generate RSA Keys (one-time)

```bash
mkdir -p keys
# Generate 2048-bit RSA private key
openssl genrsa -out keys/jwt-private.pem 2048
# Extract public key
openssl rsa -in keys/jwt-private.pem -pubout -out keys/jwt-public.pem
# ⛔ NEVER commit keys/ directory to git
echo "keys/" >> .gitignore
```

### Step 4: Set Up Databases

```bash
# Start PostgreSQL
brew services start postgresql@16   # macOS
# OR: sudo systemctl start postgresql  # Linux

# Create all schemas (uses local postgres superuser)
bash scripts/local-dev-setup.sh
```

### Step 5: Start Docker Infrastructure

```bash
# Start Redis, Kafka, MailHog, MinIO
docker compose -f infrastructure/docker/docker-compose.yml \
  --env-file ../../.env up -d redis kafka mailhog minio
```

### Step 6: Build & Start All Services

```bash
# Full build + start (first time)
bash scripts/start-all.sh

# Subsequent starts (skip build if source unchanged)
bash scripts/start-all.sh --no-build
```

### Step 7: Install Frontend Dependencies

```bash
cd frontend/web
npm install
# Frontend starts automatically via start-all.sh, or manually:
npm run dev
```

### Step 8: Verify Everything is Running

```bash
bash scripts/health-check.sh
```

Expected output:
```
✅ PostgreSQL         localhost:5432     UP
✅ Redis              localhost:6379     UP
✅ Kafka              localhost:9092     UP
✅ auth-svc           localhost:8182     UP
✅ api-gateway        localhost:8180     UP
✅ student-gateway    localhost:8089     UP
✅ frontend           localhost:3000     UP
... (all 15 services UP)
```

---

## 3. Daily Development Workflow

### Start

```bash
# 1. Ensure Postgres is running
brew services start postgresql@16

# 2. Ensure Docker Desktop is running (check menu bar)

# 3. Start services (auto-detects + rebuilds changed services)
cd /path/to/school-APK
bash scripts/start-all.sh --no-build
```

### Stop

```bash
bash scripts/stop-all.sh
```

### Rebuild a specific service

```bash
# After changing Java source in one service:
mvn package -pl services/auth-svc -am -DskipTests -q
bash scripts/restart-svc.sh auth-svc
```

### Load environment variables (for manual API testing)

```bash
while IFS='=' read -r key val; do [[ -z "$key" || "$key" == \#* ]] && continue; export "$key=$val"; done < .env
```

### Run integration tests

```bash
# All IT tests (requires Docker Desktop running)
mvn verify -Pit -DskipTests=false

# Specific service
mvn verify -pl services/assess-svc -Pit
```

### Run E2E tests

```bash
cd frontend/web
CAPTCHA_E2E_BYPASS_TOKEN=E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD \
  npx playwright test tests/e2e/institution-portal.spec.ts
```

---

## 4. Service Architecture & Ports

### Backend Services

| Service | Port | Database | Notes |
|---|---|---|---|
| **api-gateway** | **8180** | — | Main entry point for admin/parent/teacher |
| **student-gateway** | **8089** | — | Student-specific routes |
| **auth-svc** | 8182 | auth_db | JWT issuance, Google/GitHub OAuth, CAPTCHA |
| **center-svc** | 8083 | center_db | Centers, staff, jobs, banners, library |
| **assess-svc** | 8084 | assess_db | Exams, assignments, questions |
| **mentor-svc** | 8088 | mentor_db | Teacher profiles, sessions |
| **parent-svc** | 8082 | parent_db | Parent profiles, child linking, fees |
| **student-profile-svc** | 8090 | student_db | Student profiles |
| **psych-svc** | — | psych_db | Psychometric assessments |
| **notification-svc** | 8094 | notification_db | SSE + SMS notifications |
| **ai-gateway-svc** | 8086 | — | AI provider abstraction |
| **ai-mentor-svc** | — | ai_mentor_db | AI study mentor |
| **career-oracle-svc** | — | — | Career recommendations |
| **performance-svc** | — | — | Analytics |
| **exam-tracker-svc** | — | — | Exam scheduling |

### Infrastructure

| Service | Port | Purpose |
|---|---|---|
| PostgreSQL | 5432 | Primary database (LOCAL, not Docker) |
| Redis | 6379 | CAPTCHA, refresh tokens, session cache |
| Kafka | 9092 | Event bus for notifications |
| Kafka UI | — | Dev-only browser UI |
| MinIO API | 9002 | S3-compatible document storage (LOCAL) |
| MinIO Console | 9003 | MinIO web UI (LOCAL) |
| MailHog SMTP | 1025 | Local email testing |
| MailHog UI | 8025 | View sent emails |

### Frontend

| Route | Role | Description |
|---|---|---|
| `/login` | All | Login page |
| `/register` | All | Registration |
| `/dashboard` | STUDENT | Student home |
| `/admin` | ADMIN | Admin portal (tabbed) |
| `/mentor-portal` | TEACHER | Teacher portal |
| `/parent` | PARENT | Parent portal |
| `/assignments` | STUDENT | Assignment list |
| `/psychometric` | STUDENT | Psychometric test |
| `/jobs` | ALL | Job board (read-only) |
| `/lab` | STUDENT | AI Project Lab |

---

## 5. Security Configuration

### ⛔ Critical Security Rules

1. **NEVER commit `.env`** to git. It's in `.gitignore`. Verify: `git status .env` should show nothing.

2. **NEVER commit RSA keys** (`keys/jwt-private.pem`, `keys/jwt-public.pem`). Generate fresh keys per environment.

3. **NEVER reuse `CAPTCHA_E2E_BYPASS_TOKEN`** in production. Set it to empty string or remove from production env.

4. **NEVER use `KAFKA_AUTO_CREATE_TOPICS_ENABLE=true`** in production. Topics must be created explicitly.

5. **Change all default passwords** before any deployment:
   - MinIO: `minioadmin / minioadmin123` → strong password
   - Redis: set a strong password in `REDIS_PASSWORD`
   - pgAdmin: change `PGADMIN_PASSWORD`

### JWT Security

```bash
# The private key is the crown jewel — protect it
chmod 600 keys/jwt-private.pem
chmod 644 keys/jwt-public.pem

# In production: use a secrets manager (AWS Secrets Manager, HashiCorp Vault)
# Never store private key in filesystem in production
```

### Database Security

```bash
# Each service has its own DB user with minimal privileges
# Service user can: SELECT, INSERT, UPDATE, DELETE
# Service user CANNOT: ALTER TABLE, DROP, CREATE EXTENSION

# Verify current permissions
psql -U srikanth -c "\du" assess_db
```

### CORS Configuration

```yaml
# api-gateway application.yml (production)
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "https://your-production-domain.com"  # ⛔ NOT "*"
            allowedMethods: ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
            allowCredentials: true
```

---

## 6. Production Deployment Checklist

### Pre-Deployment (Security)

```bash
# 1. Generate fresh RSA keys for production
openssl genrsa -out keys/jwt-private.prod.pem 4096   # use 4096 for production
openssl rsa -in keys/jwt-private.prod.pem -pubout -out keys/jwt-public.prod.pem

# 2. Set production env variables
JWT_ISSUER=https://api.yourdomain.com
JWT_JWKS_URI=https://api.yourdomain.com/api/v1/auth/jwks
JWT_ACCESS_TOKEN_EXPIRY_SECONDS=7200
JWT_REFRESH_TOKEN_EXPIRY_SECONDS=86400   # reduce to 1 day for production

# 3. Verify .env is NOT staged
git status | grep .env   # should be empty

# 4. Remove bypass tokens
CAPTCHA_E2E_BYPASS_TOKEN=   # empty string
```

### Build for Production

```bash
# Build all JARs
mvn clean package -DskipTests -q

# Verify all JARs built
ls services/*/target/*.jar | wc -l   # should show 15

# Frontend production build
cd frontend/web
npm run build
# Output: frontend/web/dist/
```

### Start Production Services

```bash
# Use production profile
export SPRING_PROFILES_ACTIVE=prod

# Start with production env file
bash scripts/start-all.sh --prod
```

### Verify Production Health

```bash
for port in 8180 8089 8182 8083 8084 8088 8082 8090 8094 8086; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health)
  echo "Port $port: $STATUS"
done
```

### Database Migration (Production)

```bash
# Migrations run automatically on service startup via Flyway
# For DDL migrations (CHECK constraints, ALTER TABLE): run manually first
psql -U postgres -d assess_db -f services/assess-svc/src/main/resources/db/migration/assess/V12__make_assignment_marks_nullable.sql

# Verify migrations applied
psql -U postgres -d assess_db -c "SELECT version, description, success FROM assess_schema.flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"
```

---

## 7. Secrets Management

### Development (.env file — local only)

```
# ⛔ NEVER share or commit .env
# Share .env.example with placeholder values instead
```

### Staging / Production Recommendations

| Secret | Recommended Storage |
|---|---|
| DB passwords | AWS Secrets Manager / HashiCorp Vault |
| RSA private key | AWS Secrets Manager / KMS |
| Redis password | Environment variable via secrets manager |
| MinIO credentials | AWS IAM roles (use S3 instead of MinIO) |
| JWT signing key | AWS KMS for signing |
| Twilio credentials | Environment variable via secrets manager |
| GitHub OAuth secret | Environment variable |
| Google OAuth secret | Environment variable |

### Environment Variable Injection (Docker/K8s)

```yaml
# kubernetes secret
apiVersion: v1
kind: Secret
metadata:
  name: edutech-secrets
type: Opaque
stringData:
  JWT_PRIVATE_KEY_PATH: /secrets/jwt-private.pem
  REDIS_PASSWORD: "your-strong-password"
  # ... etc
```

---

## 8. Common Issues & Fixes

### "Connection refused" on any service port
```bash
# Check if service is running
ps aux | grep service-name | grep -v grep
# Check logs
tail -50 logs/service-name.log | grep -v Loki
# Restart
bash scripts/restart-svc.sh service-name
```

### CAPTCHA returns 500 (blank spinner)
```bash
# Redis is down — restart Docker Desktop from menu bar, then:
docker compose -f infrastructure/docker/docker-compose.yml up -d redis
```

### Assignment creation returns 422
```bash
# Stale JAR or V12 migration not applied
mvn package -pl services/assess-svc -am -DskipTests -q
bash scripts/restart-svc.sh assess-svc
```

### "Could not find a valid Docker environment" (IT tests)
```bash
# pom.xml must have this in maven-failsafe-plugin:
# <argLine>-Dapi.version=1.47</argLine>
# Docker Desktop 4.60+ requires API version 1.44+
```

### Services return 403 after JAR rebuild
```bash
# Stale JAR has old Role enum — rebuild ALL services that include Role.java
bash scripts/start-all.sh --no-build   # auto-detects stale JARs
```

### Flyway "checksum mismatch" on startup
```bash
# Update checksum in flyway_schema_history
psql -U srikanth -d {service}_db -c "
  UPDATE {schema}.flyway_schema_history
  SET checksum = {correct_checksum}
  WHERE version = '{version}';
"
# Get correct checksum from logs: "Resolved locally: XXXXXXXX"
```

### Disk full (ENOSPC)
```bash
bash scripts/cleanup.sh --force   # cleans logs, screenshots, build artifacts
```

### JWT "already logged out" after 15 minutes
```bash
# Fixed in .env: JWT_ACCESS_TOKEN_EXPIRY_SECONDS=7200 (2 hours)
# Restart auth-svc for change to take effect
```

---

## 9. Team Access & Branch Strategy

### Repository Access

```
Repository: https://github.com/Srikanthnexushub/school-APK
```

**Requesting access:**
1. Contact repo owner (Srikanth) with your GitHub username
2. You will be added as a collaborator
3. Clone using SSH for daily work: `git clone git@github.com:Srikanthnexushub/school-APK.git`

### Branch Strategy

```
main              ← production-ready; protected; requires PR
  └── feature/    ← new features (feature/job-board, feature/ai-mentor)
  └── fix/        ← bug fixes (fix/assignment-422, fix/jwt-ttl)
  └── hotfix/     ← urgent production fixes (merge directly to main with review)
```

### Commit Convention

```
feat: short description (Fix #N)    ← new feature
fix: short description (Fix #N)     ← bug fix
docs: short description             ← docs only
refactor: short description         ← no behavior change
test: short description             ← test additions
```

### Pull Request Rules

1. All PRs target `main`
2. TypeScript check must pass (pre-commit hook)
3. All IT tests must pass: `mvn verify -Pit`
4. At least 1 reviewer approval required
5. Squash merge preferred for clean history

### Pre-Commit Hook

The repository has a pre-commit hook that runs TypeScript compilation:
```bash
# Install on fresh clone:
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
cd frontend/web
echo "🔍 Running TypeScript check..."
if npx tsc --noEmit --skipLibCheck 2>&1 | grep -v "\.spec\." | grep -v "\.test\." | grep "error TS"; then
  echo "❌ TypeScript errors found — commit blocked"
  exit 1
fi
echo "✅ TypeScript OK"
EOF
chmod +x .git/hooks/pre-commit
```

---

## Appendix: Test Credentials (Dev Only — NEVER use in production)

| Role | Email | Password |
|---|---|---|
| INSTITUTION_ADMIN | `superadmin@nexused.com` | `Test@12345` |
| CENTER_ADMIN | `institute@nexused.com` | `Test@12345` |
| TEACHER | `teacher1@test.com` | `Test@12345` |
| STUDENT | `journey.student@test.com` | `Test@12345` |
| PARENT | `ravi.parent@test.com` | `Test@12345` |

**Captcha bypass (dev/E2E only):**
```
E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD:bypass
```

---

*Prepared: 2026-03-24 | Repository: https://github.com/Srikanthnexushub/school-APK*
*⛔ This document contains development credentials — do NOT distribute publicly*
