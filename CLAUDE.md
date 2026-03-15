# EduTech AI Platform — Claude Code Session Guide

## ⛔ PERMISSION RULE — READ THIS FIRST
**ANY modification, refactor, bug fix, or change to ANY code, test, config, or migration file requires EXPLICIT USER PERMISSION before acting.**
- Ask first. Act only after the user says yes.
- This includes "small" fixes, dependency updates, and "obvious" improvements.
- Memory files at `~/.claude/projects/.../memory/` track all frozen fixes — read `frozen-fixes.md` before touching any file.

---

## Starting a Fresh Session — What to Say

You don't need to explain anything. Just tell Claude what you want to do:

**Examples of good session openers:**
- `"Check if all services are running"`
- `"I want to work on the parent portal add-child feature"`
- `"There's a bug on the login page — [describe it]"`
- `"Run a full feature check"`

Claude will read this file + memory automatically and have full context.

---

## Start Services (run this BEFORE opening Claude Code)

```bash
# 1. Make sure Docker Desktop is running (check macOS menu bar)
# 2. From the project root:
bash scripts/start-all.sh --no-build    # fastest (jars already built)
bash scripts/start-all.sh               # full build + start
bash scripts/start-all.sh --infra-only  # only Docker infra (Postgres/Redis/Kafka)
```

**If only Redis is down (CAPTCHA broken):**
```bash
# Load env first
while IFS='=' read -r key val; do [[ -z "$key" || "$key" == \#* ]] && continue; export "$key=$val"; done < .env
docker compose -f infrastructure/docker/docker-compose.yml up -d redis
```

---

## Service Ports Quick Reference

| Service | Port | Health |
|---|---|---|
| **Frontend (Vite)** | **3000** | http://localhost:3000 |
| **api-gateway** | **8180** | /actuator/health |
| **student-gateway** | **8089** | /actuator/health |
| **auth-svc** | 8182 | /actuator/health |
| center-svc | 8083 | /actuator/health |
| parent-svc | 8082 | /actuator/health |
| student-profile-svc | 8090 | /actuator/health |
| assess-svc | 8084 | /actuator/health |
| mentor-svc | 8088 | /actuator/health |
| notification-svc | 8094 | /actuator/health |
| ai-gateway-svc | 8086 | /actuator/health |

---

## Test User Credentials (all passwords: `Test@12345`)

| Role | Email | Notes |
|---|---|---|
| **Parent** | `ravi.parent@test.com` | userId `bd7d02da-11e3-4bda-9795-41d3c93bac69` |
| **Student** | `qa-test@nexused.dev` | userId `50d63f9c-9b9c-4737-a66a-22b29dad42a1` |
| **Teacher** | `teacher1@test.com` | — |
| **Admin** | `admin@test.com` | — |

---

## Common Startup Problems & Fixes

### CAPTCHA not loading (blank / spinning)
**Symptom:** CAPTCHA widget shows spinner forever, `GET /api/v1/captcha/challenge` returns 500.
**Cause:** Redis container down — `docker logs edutech-redis` or `docker ps` returns `EOF`.
**Fix:**
1. Docker Desktop daemon has crashed → restart Docker Desktop from the macOS **menu bar**
2. After Docker is back: `docker compose -f infrastructure/docker/docker-compose.yml up -d redis`
3. Do NOT try `redis-server` locally — Docker Desktop holds port 6379.

### Login form pre-fills with old credentials
**Fixed permanently** in commit `b54547a` (Fix #40). `autoComplete="new-password"` on the password field prevents browser autofill.

### `docker ps` returns `EOF`
Docker Desktop daemon has crashed. Restart Docker Desktop from the macOS menu bar. Programmatic restart does NOT work.

### Services return 401 for all requests
JWT expired (15 min TTL). Re-login. If login itself fails → CAPTCHA issue (see above).

### auth-svc health shows `DOWN`
Usually means Redis is unreachable. Fix Redis first (see above), then auth-svc recovers automatically.

### `mvn test-compile` fails
Check for Java compilation errors in test files. Run: `mvn test-compile --no-transfer-progress 2>&1 | grep ERROR`

---

## Architecture Rules (NEVER violate)

- **Hexagonal architecture**: domain → application → infrastructure only. No Spring annotations in `domain/`.
- **JWT auth**: every service validates the JWT with the RSA public key. No service calls another without the JWT header.
- **All routes go through gateways**: frontend → api-gateway (8180) or student-gateway (8089). Never call services directly.
- **Captcha bypass** (E2E/dev only): `captchaToken: "E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD:bypass"` — note the `:` separator.
- **Page<T> extraction**: Spring returns paginated responses. Always: `Array.isArray(d) ? d : (d.content ?? [])`.

---

## Key Features Implemented (Frozen — Ask Before Touching)

| Feature | File(s) | Commit |
|---|---|---|
| Login autofill fix | `LoginPage.tsx` | b54547a |
| Add Child modal (DOB, institution dropdown, AI Smart Find) | `ParentChildrenPage.tsx` | 6c45f2d |
| Register page (roles, layout, PARENT/CENTER_ADMIN flows) | `RegisterPage.tsx` | 7356635 |
| OTP-based parent-child linking | `ParentChildrenPage.tsx`, `SettingsPage.tsx` | ac81927 |
| Password recovery + MFA/TOTP | `LoginPage.tsx`, `SettingsPage.tsx` | 5bd149f |
| Teacher onboarding + bulk import | `center-svc`, `AdminPendingTeachersPage.tsx` | 8bd6075 |
| Real-time notifications (SSE + SMS) | `notification-svc`, `AppLayout.tsx` | 275e718 |
| Google Sign-In | `LoginPage.tsx`, `RegisterPage.tsx` | 944f801 |

Full frozen fix list: `~/.claude/projects/.../memory/frozen-fixes.md` (40 fixes as of b54547a)

---

## Env File

`.env` in project root. Load in shell:
```bash
while IFS='=' read -r key val; do [[ -z "$key" || "$key" == \#* ]] && continue; export "$key=$val"; done < .env
```

Key values:
- `AI_DEFAULT_PROVIDER=OPENROUTER` (uses `arcee-ai/trinity-large-preview:free`)
- `TWILIO_ACCOUNT_SID=dev_placeholder` → SMS logs only, no real sends
- `CAPTCHA_E2E_BYPASS_TOKEN=E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD`
