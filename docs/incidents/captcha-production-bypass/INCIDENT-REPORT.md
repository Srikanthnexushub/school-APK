# Incident Report: CAPTCHA Bypass Active in Production (EC2)

**Date:** 2026-03-28
**Severity:** HIGH — All users on http://13.203.158.45 could not see a real CAPTCHA image; the security check was cosmetically broken (showed a solid green 1×1 pixel rectangle) and functionally bypassable.
**Status:** RESOLVED — Permanent fix applied.
**Fix Reference:** Fix #134 in `memory/frozen-fixes.md`

---

## 1. What Was the Problem

When a user visited `http://13.203.158.45/login`, the CAPTCHA security check widget displayed a **solid green rectangle** instead of a real distorted-text image. The user could not read any characters and therefore could not log in.

Additionally, an attacker who knew the bypass token value could log in with **zero CAPTCHA friction** by submitting:
```
captchaToken: "E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD:anything"
```
This would pass the server-side verifier without any Redis lookup.

---

## 2. Root Cause Analysis

### Layer 1 — Wrong Environment Variable in Production

The file `/opt/apps/.env.prod` on EC2 (loaded by all Java services at startup via systemd `EnvironmentFile=`) contained:

```env
CAPTCHA_E2E_BYPASS_TOKEN=E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD
```

This variable is intended **exclusively for local automated E2E test runs**. Its comment in the local `.env` explicitly states:
> "E2E test bypass token — empty in prod, set for automated E2E runs only"

When this token is non-empty, the deployed auth-svc build returned a **bypass challenge** from `GET /api/v1/captcha/challenge`:
- `id` = the bypass token string (`"E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD"`)
- `imageDataUri` = a 1×1 pixel green PNG (118 characters base64) — NOT a real captcha

This is likely because the EC2 WAR was an older build of `LocalCaptchaService` that included bypass logic in the `generate()` method. The current local code has this removed from the generator (bypass lives only in the verifier), but the deployed binary on EC2 had it in both.

### Layer 2 — Both Services Loaded the Bypass Token

The `EnvironmentFile=/opt/apps/.env.prod` directive in the systemd unit files means **every service** that uses that file loaded the bypass token at startup. Both `auth-svc` (port 8182) and `api-gateway` (port 8180) had `CAPTCHA_E2E_BYPASS_TOKEN` set in their process environment. Even though api-gateway has no captcha bypass code, it needed to be restarted so its env was clean (and to avoid any future regression if bypass logic is ever added at the gateway layer).

### Layer 3 — Browser HTTP Cache

After clearing the bypass token and restarting `auth-svc`, the browser (and Playwright test browser) continued to show the green rectangle. Investigation showed:

```js
// Browser fetch with no cache control:
fetch('/api/v1/captcha/challenge')  →  id: "E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD", srcLen: 118

// Browser fetch with cache: 'no-store':
fetch('/api/v1/captcha/challenge?bust=...', { cache: 'no-store' })  →  id: "<real-uuid>", imageLen: 15838
```

The browser had cached the bypass response (HTTP 200 without explicit `Cache-Control: no-store`) and kept serving it from cache on subsequent page loads. The React app's Axios call to `/api/v1/captcha/challenge` hit the browser cache, not the server.

---

## 3. Timeline

| Time | Event |
|------|--------|
| Before fix | EC2 `.env.prod` had `CAPTCHA_E2E_BYPASS_TOKEN=E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD` |
| Before fix | auth-svc (PID started Mar27) loaded bypass token → challenge endpoint returned 1×1 green PNG |
| Before fix | api-gateway (PID 34538 started Mar27) loaded bypass token |
| Before fix | Browser cached the 1×1 bypass challenge response |
| Step 1 | SSH → `sed -i` → cleared `CAPTCHA_E2E_BYPASS_TOKEN=` in `/opt/apps/.env.prod` |
| Step 2 | `sudo systemctl restart edutech-auth-svc` → health: UP |
| Step 3 | Direct `curl localhost:8182/api/v1/captcha/challenge` → id: real UUID, imageLen: 16,594 ✅ |
| Step 4 | Browser still showing bypass (cache hit) |
| Step 5 | `sudo systemctl restart edutech-api-gateway` → health: UP |
| Step 6 | Browser still showing bypass (cache hit) |
| Step 7 | Added `?t=${Date.now()}` cache-bust to CaptchaWidget.tsx API call |
| Step 8 | Built frontend: `npx vite build` (0 errors) |
| Step 9 | Deployed to EC2 nginx via SCP |
| Step 10 | Browser: naturalWidth=220, naturalHeight=80, srcLen=15,466 ✅ Real captcha visible |

---

## 4. Permanent Fixes Applied

### Fix A — EC2 Production `.env.prod` (Server)
**File:** `/opt/apps/.env.prod` on EC2
**Change:** `CAPTCHA_E2E_BYPASS_TOKEN=E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD` → `CAPTCHA_E2E_BYPASS_TOKEN=`
**Effect:** auth-svc returns real CAPTCHA images. Verifier rejects all bypass token submissions.

### Fix B — Frontend Cache-Bust (Client)
**File:** `frontend/web/src/components/CaptchaWidget.tsx`
**Change:** Line 35:
```ts
// BEFORE (cacheable):
const res = await api.get('/api/v1/captcha/challenge');

// AFTER (permanent cache-bust):
const res = await api.get(`/api/v1/captcha/challenge?t=${Date.now()}`);
```
**Effect:** Every challenge request has a unique URL. Browser HTTP cache can never serve a stale response. Each page load, each remount, and each "Refresh" click fetches a brand-new challenge from the server. This fix survives browser upgrades, CDN caching, and any future bypass incident.

### Fix C — Both Services Restarted
Both `edutech-auth-svc` and `edutech-api-gateway` were restarted after clearing the env var to ensure no in-process copy of the bypass token remained.

---

## 5. Verification

Post-fix verification via Playwright browser evaluation:

```js
// Image dimensions confirm real 220×80 PNG (not 1×1 bypass pixel):
{ naturalWidth: 220, naturalHeight: 80, srcLen: 15466 }

// Direct API test through nginx (browser path):
curl http://localhost/api/v1/captcha/challenge
// → id: "3539b5ff-5c6c-4e35-a0e1-dbea439c2c86" (real UUID)
// → imageLen: 15854 (real image, not 118-byte bypass)
```

Screenshot: `captcha-fixed-final.png` — real distorted text visible (`YT8YZ6`).

---

## 6. Complete Route Audit (Post-Fix)

All gateway routes verified correct as of Fix #134:

### api-gateway (port 8180 — the ONLY gateway reachable via nginx)

| Route ID | Path | Backend |
|---|---|---|
| auth-svc | `/api/v1/auth/**` | auth-svc:8182 |
| auth-svc-otp | `/api/v1/otp/**` | auth-svc:8182 |
| **auth-svc-captcha** | **`/api/v1/captcha/**`** | **auth-svc:8182** |
| parent-svc | `/api/v1/parents/**` | parent-svc:8082 |
| parent-svc-copilot | `/api/v1/copilot/**` | parent-svc:8082 |
| center-svc | `/api/v1/centers/**` | center-svc:8083 |
| center-svc-jobs | `/api/v1/jobs/**` | center-svc:8083 |
| center-svc-banners | `/api/v1/banners/**` | center-svc:8083 |
| center-svc-library | `/api/v1/library/**` | center-svc:8083 |
| assess-svc-assignments | `/api/v1/assignments/**` | assess-svc:8084 |
| assess-svc-student-assignments | `/api/v1/students/*/assignments` | assess-svc:8084 |
| student-profile-svc | `/api/v1/students/**` | student-profile-svc:8090 |
| study-plans | `/api/v1/study-plans/**` | ai-mentor-svc:8093 |
| reminders | `/api/v1/reminders/**` | ai-mentor-svc:8093 |
| doubts | `/api/v1/doubts/**` | ai-mentor-svc:8093 |
| recommendations | `/api/v1/recommendations/**` | ai-mentor-svc:8093 |
| exam-tracker-svc | `/api/v1/exam-tracker/**` | exam-tracker-svc:8091 |
| performance-svc | `/api/v1/performance/**` | performance-svc:8092 |
| career-oracle-profiles | `/api/v1/career-profiles/**` | career-oracle-svc:8087 |
| career-oracle-recommendations | `/api/v1/career-recommendations/**` | career-oracle-svc:8087 |
| career-oracle-colleges | `/api/v1/college-predictions/**` | career-oracle-svc:8087 |
| mentor-svc | `/api/v1/mentors/**` | mentor-svc:8088 |
| mentor-sessions | `/api/v1/mentor-sessions/**` | mentor-svc:8088 |
| assess-svc-exams | `/api/v1/exams/**` | assess-svc:8084 |
| assess-svc-questions | `/api/v1/questions/**` | assess-svc:8084 |
| assess-svc-grades | `/api/v1/grades/**` | assess-svc:8084 |
| psych-svc | `/api/v1/psych/**` | psych-svc:8085 |
| ai-gateway-svc | `/api/v1/ai/**` | ai-gateway-svc:8086 |
| notification-svc | `/api/v1/notifications/**` | notification-svc:8094 |

### student-gateway (port 8089 — NOT reachable from browser; internal only)

No CAPTCHA routes (correct — captcha is auth-svc only, and student-gateway is internal).

---

## 7. How to Prevent Recurrence

### Rule 1 — CAPTCHA_E2E_BYPASS_TOKEN must always be EMPTY in production

**Check before every EC2 deployment:**
```bash
ssh ec2-user@13.203.158.45 "grep CAPTCHA_E2E_BYPASS_TOKEN /opt/apps/.env.prod"
# Must output: CAPTCHA_E2E_BYPASS_TOKEN=
# If it shows anything else — STOP. Clear it before starting services.
```

### Rule 2 — If `CAPTCHA_E2E_BYPASS_TOKEN` ever gets re-set in production, restart BOTH services

```bash
sudo systemctl restart edutech-auth-svc
sudo systemctl restart edutech-api-gateway
```

### Rule 3 — Frontend cache-bust is permanent

The `?t=${Date.now()}` parameter on the captcha challenge URL in `CaptchaWidget.tsx` must NEVER be removed. It is the last line of defence against browser-cached stale responses.

### Rule 4 — Any new service added that reads `.env.prod` must be audited for bypass token usage

The shared EnvironmentFile approach means ALL services see ALL env vars. Any new service must be reviewed to ensure it does not accidentally read or act on `CAPTCHA_E2E_BYPASS_TOKEN`.

### Rule 5 — E2E tests MUST use local `.env`, never production `.env.prod`

E2E test pipelines run locally or in CI with `.env` (not `.env.prod`). Never copy or inherit production env files for test runs.

---

## 8. Code Locations (Canonical)

| File | What It Does | Bypass-Relevant |
|---|---|---|
| `services/auth-svc/.../LocalCaptchaService.java` | Generates real challenges (UUID + 220×80 PNG) | NO bypass — always generates real image |
| `services/auth-svc/.../LocalCaptchaVerifierAdapter.java` | Verifies challenge answer at login | YES — bypass active only when `CAPTCHA_E2E_BYPASS_TOKEN` is non-empty |
| `services/auth-svc/.../CaptchaController.java` | HTTP endpoint `GET /api/v1/captcha/challenge` | Delegates to LocalCaptchaService only |
| `services/auth-svc/.../CaptchaRedisStore.java` | Stores challenges in Redis (5-min TTL, single-use) | No bypass |
| `services/api-gateway/src/main/resources/application.yml` | Routes `/api/v1/captcha/**` → auth-svc:8182 | No bypass code |
| `frontend/web/src/components/CaptchaWidget.tsx` | React widget — fetches challenge, shows image, handles input | Cache-bust `?t=` param permanently added |
| `frontend/web/src/pages/auth/LoginPage.tsx` | Mounts CaptchaWidget, sends token to login API | No bypass |
| `/opt/apps/.env.prod` (EC2) | Production env file loaded by ALL systemd services | `CAPTCHA_E2E_BYPASS_TOKEN=` (empty — MUST stay empty) |
| `.env` (local dev) | Development env — bypass token set for local E2E tests | `CAPTCHA_E2E_BYPASS_TOKEN=E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD` |

---

## 9. Strict Enforcement — What Must NEVER Change

⛔ **NEVER** set `CAPTCHA_E2E_BYPASS_TOKEN` to a non-empty value in `/opt/apps/.env.prod` on EC2.

⛔ **NEVER** remove the `?t=${Date.now()}` from the captcha challenge URL in `CaptchaWidget.tsx`.

⛔ **NEVER** copy `.env` (local dev) to EC2 as `.env.prod` — they are different files with different bypass token values.

⛔ **NEVER** restart only one service after clearing `.env.prod` — both `auth-svc` AND `api-gateway` must be restarted.

⛔ **NEVER** skip the verification step after any captcha-related change:
```bash
curl http://localhost/api/v1/captcha/challenge | python3 -c "import sys,json; d=json.load(sys.stdin); print('id:', d['id'][:40], '| imageLen:', len(d.get('imageDataUri','')))"
# id must be a UUID, imageLen must be > 10000
```
