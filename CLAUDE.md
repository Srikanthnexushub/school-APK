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

### Frontend `ERR_CONNECTION_REFUSED` on localhost:3000
**Symptom:** Browser or Playwright cannot connect to `http://localhost:3000` even though Vite is running.
**Cause:** Vite was binding only to IPv6 `[::1]:3000`, not IPv4 `127.0.0.1:3000`. `localhost` resolves to IPv4 on many systems, so the connection was refused.
**Fixed permanently** in `vite.config.ts` by adding `host: true` to the server block — Vite now binds to `0.0.0.0` (all interfaces). Also fixed `start-all.sh` to print `http://localhost:3000` (was wrongly showing `5173`).

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
| District/state/country autocomplete (all roles) | `indiaLocations.ts`, `ParentProfilePage.tsx`, `SettingsPage.tsx`, `RegisterPage.tsx` | see frozen-fixes |
| Copilot psychometric fix (real data, no hallucination) | `CopilotService.java` | see frozen-fixes |
| Adaptive psychometric questions (board/class/gender/stream) | `PsychometricPage.tsx` | see frozen-fixes |
| + Add Parent button (student Settings, top of Profile tab) | `SettingsPage.tsx` | see frozen-fixes |
| AI Project Lab `/lab` (student portal) | `ProjectLabPage.tsx`, `router.tsx`, `AppLayout.tsx` | see frozen-fixes |
| Institution name in Add Child + Register (optional free-text) | `ParentChildrenPage.tsx`, `RegisterPage.tsx` | see frozen-fixes |
| center-svc branch/board fields + V11 migration | `CoachingCenter.java`, `CenterService.java`, `RegisterPage.tsx` | see frozen-fixes |
| Danger Zone → single Delete Account button (all roles) | `SettingsPage.tsx` | see frozen-fixes |
| Teacher reg: institution dropdown, subjects dropdown, district + country | `RegisterPage.tsx`, center-svc, mentor-svc | 5d86803 |
| Country field on all Register roles (Student/Parent/Teacher/Institution) | `RegisterPage.tsx` | 5d86803 |
| Register autofill blocked (autoComplete="new-password" email+password) | `RegisterPage.tsx` | 5d86803 |
| center-svc V12: district on teachers table | `V12__add_district_to_teachers.sql`, `Teacher.java` | 5d86803 |
| mentor-svc V6: district on mentor_profiles table | `V6__add_district_to_mentor_profiles.sql`, `MentorProfile.java` | 5d86803 |
| Staff Portal (CENTER_ADMIN) — full role-based staff management + AI | `AdminStaffPage.tsx`, `CreateStaffModal.tsx`, `staffConstants.ts`, `useStaffAI.ts`, `StaffController.java`, `StaffService.java`, `StaffRoleType.java`, `V13__add_staff_profile_fields.sql` | see frozen-fixes |
| Register UX — role dropdown, Country→State→City, email/password at bottom, address line 1+2, board multi-select, pincode | `RegisterPage.tsx`, `indiaLocations.ts` | e843293 |
| Staff modal — Subjects hidden for non-teaching roles (LIBRARIAN/COUNSELOR/LAB_ASSISTANT/SPORTS_COACH/ADMIN_STAFF), District removed | `CreateStaffModal.tsx` | 4458d30 |
| Pre-commit hook — TypeScript check blocks broken commits | `.git/hooks/pre-commit` | 4458d30 |
| Jobs feature — My Postings (full CRUD + stat cards + inline status transitions) + Job Board (paginated public view across all institutions, city/role/type filters) | `AdminJobsPage.tsx`, `PostJobModal.tsx`, `jobConstants.ts`, `AdminPortalPage.tsx`, center-svc: `JobPosting.java`, `JobPostingService.java`, `JobPostingController.java`, `V14+V15 migrations`, `SpringDataJobPostingRepository.java` (CAST fix), api-gateway route `/api/v1/jobs/**` | 1b19097 |

Full frozen fix list: `~/.claude/projects/.../memory/frozen-fixes.md` (60+ fixes)

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
