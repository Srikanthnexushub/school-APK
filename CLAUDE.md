# EduTech AI Platform — Claude Code Session Guide

## ⛔ PERMISSION RULE — READ THIS FIRST
**ANY modification, refactor, bug fix, or change to ANY code, test, config, or migration file requires EXPLICIT USER PERMISSION before acting.**
- Ask first. Act only after the user says yes.
- This includes "small" fixes, dependency updates, and "obvious" improvements.
- Memory files at `~/.claude/projects/.../memory/` track all frozen fixes — read `frozen-fixes.md` before touching any file.

---

## 🧠 THINK BEFORE BUILDING — LAYOUT & ARCHITECTURE CHECKLIST

Before building any new UI component, navigation, or feature — **stop and answer these questions first**:

1. **Layout conflict check**: Does `AppLayout.tsx` already render a sidebar/nav/header at this route? If yes, does the new component add a second one? → Fix the existing one instead.
2. **Data already exists?**: Is there an existing API, store, hook, or component that already does what's needed? → Reuse it.
3. **Where does state live?**: Should this state be local, in a store (Zustand), or server-side (React Query)?
4. **Role/permission impact**: Does this change affect what CENTER_ADMIN / INSTITUTION_ADMIN / SUPER_ADMIN / STUDENT / PARENT / TEACHER sees? Check all roles.
5. **Frozen fix conflict**: Does this touch any file mentioned in `frozen-fixes.md`? → Ask permission explicitly.

**If any answer is "yes" or "unsure" — explain the conflict to the user BEFORE writing code.**

---

## 🔧 Advanced Concepts & Patterns Used in This Codebase

### Frontend (React / TypeScript)
- **Zustand** (`useAuthStore`) — global auth state (token, user, role, centerId). Persist key: `edupath-auth`. Access: `useAuthStore(s => s.user?.role)`.
- **React Query** (`@tanstack/react-query`) — all server data fetching. Use `queryKey` arrays, `staleTime`, `enabled` guards. Pagination: `Array.isArray(d) ? d : (d.content ?? [])`.
- **Framer Motion** — page transitions (`AnimatePresence`, `motion.div`), animated tab indicators (`layoutId`). ⛔ NEVER use `-translate-x/y-1/2` with Framer Motion (it gets overridden).
- **React Router v6** — `useSearchParams` for tab state in admin portal (`?tab=xxx`). `NavLink` for active styles. `Outlet` in AppLayout.
- **`AppLayout.tsx`** — the single persistent shell for ALL roles. Left collapsible sidebar + top header + `<Outlet>`. Nav items come from `getNavItems(role)` which calls `getAdminNav(role)` for admin roles. **Never add a second sidebar inside a page rendered inside AppLayout.**
- **`createPortal`** — used for modals that need to escape `overflow-hidden` parents (e.g. AdminBannersPage modal).
- **File uploads** — ALWAYS use `<label>` wrapping `<input type="file" className="hidden">`. NEVER `div + onClick + programmatic .click()` (causes click-bubble loop).
- **SSE (Server-Sent Events)** — real-time notifications via `useNotifications` hook. Dedup by `id` to prevent badge inflation on reconnect.
- **Intersection Observer** — used in `VideoBanner.tsx` to pause video when scrolled out of view.

### Backend (Spring Boot / Java)
- **Hexagonal architecture** — domain → application → infrastructure layers. No Spring annotations in `domain/`. Ports + Adapters pattern.
- **JWT (RSA asymmetric)** — every service validates with RSA public key. 15-min TTL. Payload: `sub=userId`, `role`, `centerId`. All inter-service calls require JWT header.
- **Spring Security** — RBAC per endpoint. Empty-body 403 = Jackson deserialization failure BEFORE controller (e.g. bad enum value), NOT an auth error.
- **Spring Data JPA + Flyway** — migrations in `src/main/resources/db/migration/V{n}__description.sql`. ALWAYS increment V number. Check constraints (e.g. `chk_banner_type`) must be dropped+recreated when adding new enum values.
- **TestContainers** — integration tests spin up real Postgres (pgvector/pg16). No mocks for DB. Pattern: `@SpringBootTest` + `@ActiveProfiles("test")` + static container.
- **Kafka** — event bus for notifications. Topics defined in `event-contracts` module. Services publish/consume domain events.
- **JPQL CAST fix** — `CAST(:param AS String)` in JPQL queries to prevent `lower(bytea)` error on nullable params in Postgres.

### Infrastructure
- **Docker Compose** — all infra (Postgres, Redis, Kafka, MailHog) via `infrastructure/docker/docker-compose.yml`.
- **Maven multi-module** — ALWAYS run `mvn` from project root. Internal modules (`common-security`, `event-contracts`, `test-fixtures`) only resolve from the reactor.
- **Gateway routing** — ALL frontend → api-gateway (8180) or student-gateway (8089). Never call services directly. Routes defined in gateway `application.yml`.

---

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
bash scripts/start-all.sh --no-build    # smart: skips full build but auto-rebuilds any service whose source changed since its JAR was built
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
| **CENTER_ADMIN** | `institute@nexused.com` | centerId `6e9985dd-f029-49aa-8d22-39c42525df97` |
| **INSTITUTION_ADMIN** | `superadmin@nexused.com` | platform INSTITUTION_ADMIN (no center_id); can create centers |
| **SUPER_ADMIN** | _(no longer used for self-reg)_ | Blocked from self-registration. Existing DB row: `superadmin@nexused.com` carries role INSTITUTION_ADMIN after Fix #65. |

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

### IT tests fail with "Could not find a valid Docker environment"
**Symptom:** All `*IT.java` tests fail with `Could not find a valid Docker environment` at TestContainers startup.
**Cause:** Docker Desktop 4.60.1+ raised `MinAPIVersion` to 1.44. TestContainers' bundled (shaded) docker-java defaults to requesting `/v1.41/info` → HTTP 400 Bad Request.
**Fixed permanently** in `pom.xml` (commit `2666427`): `<argLine>-Dapi.version=1.47</argLine>` in maven-failsafe-plugin `<configuration>`. ⛔ NEVER remove this line.
**If it reappears:** Do NOT reinstall TestContainers. Check if `pom.xml` still has the `<argLine>` — if removed, restore it. If argLine is present and still failing, verify Docker Desktop is running (`docker ps`).

### IT tests fail with "Could not resolve placeholder 'APP_ENVIRONMENT'"
**Symptom:** Spring context fails to load in IT tests with `NumberFormatException` or placeholder errors for `${APP_ENVIRONMENT}`, `${PSYCH_SVC_DB_CONNECTION_TIMEOUT_MS}`, etc.
**Cause:** `application.yml` uses env vars. `application-test.yml` must override all of them. Previously these were set in `.env` loaded in the shell.
**Fixed permanently** in `services/psych-svc/src/test/resources/application-test.yml` (commit `2666427`): added `spring.datasource.hikari.*` pool settings and `management.metrics.tags.*` overrides. ⛔ NEVER remove these sections.

### `mvn test-compile` fails
Check for Java compilation errors in test files. Run: `mvn test-compile --no-transfer-progress 2>&1 | grep ERROR`

### Exam creation returns empty 403 (not an auth issue)
**Symptom:** `POST /api/v1/exams` returns 403 with empty response body.
**Cause:** `ExamMode` enum only has `STANDARD` and `CAT`. Sending `"mode": "ONLINE"` causes Jackson deserialization failure BEFORE the controller is reached. Spring Security renders the error as an empty 403.
**Fix:** Use `"mode": "STANDARD"` or `"mode": "CAT"`.

### `POST /api/v1/questions` returns 403
Wrong URL. Questions are scoped per exam.
**Fix:** Use `POST /api/v1/exams/{examId}/questions`.

### Login response has no userId / role
`POST /api/v1/auth/login` returns only `{ accessToken, refreshToken }`.
To get userId/role/centerId, decode the JWT payload:
```js
const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/')));
// payload.sub = userId, payload.role, payload.centerId
```

### DeviceFingerprint must be a JSON object, not a string
```json
"deviceFingerprint": { "userAgent": "E2E-Test/1.0", "deviceId": "e2e-001", "ipSubnet": "127.0.0.1/24" }
```
Sending a plain string → 500.

### `!` in passwords breaks bash curl
Bash history expansion interprets `!` inside double-quoted strings.
Use Python `urllib.request` for API calls with passwords that contain `!`.

### StudentAssignmentsPage shows "No center linked"
**Cause:** `auth_schema.users.center_id` is NULL for the student — JWT carries no `centerId` → store returns `undefined`.
**Fix:** `UPDATE auth_schema.users SET center_id='<centerId>' WHERE email='<student>';`

### AdminBatchesPage shows "No center found"
**Cause:** `CenterService.resolveAccessibleCenters()` queries `findByOwnerId`. If `owner_id` ≠ CENTER_ADMIN userId the list is empty.
**Fix:** `UPDATE center_schema.centers SET owner_id='<ca_userId>', admin_user_id='<ca_userId>' WHERE id='<centerId>';`

### Disk full / ENOSPC errors
**Root cause:** A single unbounded service log (e.g. `logs/performance-svc.log`) can grow to hundreds of GB.
**Permanent prevention (Fix #65):**
- `start-all.sh` runs a background `start_log_watchdog()` — checks every 10 min, truncates any `*.log` > 100 MB in-place (APFS sparse, no disk waste).
- `scripts/cleanup.sh` — manual cleanup: `bash scripts/cleanup.sh` (logs only), `--build` (Maven target/), `--all --force` (everything).
- `.gitignore` excludes: `.playwright-mcp/`, `*.png`/`*.jpg` at root, `docs/screenshots/`, `python-ai-svc/.venv/`, `dump.rdb`.
**Emergency recovery:** `bash scripts/cleanup.sh --force` then if Docker crashed restart Docker Desktop from macOS menu bar.

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
| Institution Portal E2E (22 Playwright tests) + JobPosting IT (17 Spring IT) | `tests/e2e/institution-portal.spec.ts`, `JobPostingControllerIT.java` | 7f01c7b |
| Assignments (centralized, all-role RBAC) — assess-svc V10+V11 migrations, Assignment + AssignmentSubmission domain, DRAFT→PUBLISHED→CLOSED state machine, late submission detection, 12 REST endpoints, AssignmentControllerIT (17 IT), StudentAssignmentsPage, MentorPortalAssignmentsPage, AdminAssignmentsTab | `assess-svc: V10+V11, Assignment*.java, AssignmentService, AssignmentController`; `frontend: StudentAssignmentsPage.tsx, MentorPortalAssignmentsPage.tsx, AdminAssignmentsTab.tsx` | d301e53 |
| Advertisement Banners — center-svc V16 migration, Banner domain + BannerAudience (PARENT/CENTER_ADMIN/ALL), SUPER_ADMIN CRUD, date-window filtering, AdvertisementBanner hero carousel (auto-rotate, pause-on-hover, nav dots), FooterBanner cyclic strip, integrated into ParentDashboard + AdminDashboard, SUPER_ADMIN banner management tab, BannerControllerIT (12 IT) | `center-svc: V16, Banner*.java, BannerService, BannerController`; `frontend: AdvertisementBanner.tsx, FooterBanner.tsx, AdminBannersPage.tsx` | d301e53 |
| Unified welcome greeting — "Welcome, [firstName]" on all 4 role dashboards (Student/Parent/Admin/Mentor) | `StudentDashboardPage.tsx`, `ParentDashboardPage.tsx`, `AdminDashboardPage.tsx` | 039cd74 |
| Running Ticker Banner (BannerType HERO/TICKER) — third-party advertiser marquee strip, V17 migration, TickerBanner component, admin form type selector | `center-svc: V17, BannerType.java, Banner.java, BannerService`; `frontend: TickerBanner.tsx, AdminBannersPage.tsx, ParentDashboardPage.tsx, AdminDashboardPage.tsx` | d0ee154 |
| VIDEO Banner type — autoplay muted video advertisements, V18 migration (video_url column), V19 migration (extends chk_banner_type constraint to include VIDEO — REQUIRED or POSTs fail with 23514 check violation), BannerType.VIDEO, VideoBanner.tsx (Intersection Observer pause-on-scroll, real-time progress bar, play/pause+mute controls, AnimatePresence), VIDEO option in admin form with rose badge | `center-svc: V18, V19__extend_banner_type_video.sql, BannerType.java, Banner.java, BannerResponse, CreateBannerRequest, UpdateBannerRequest, BannerService, BannerControllerIT`; `frontend: VideoBanner.tsx, AdvertisementBanner.tsx, AdminBannersPage.tsx, AdminDashboardPage.tsx, ParentDashboardPage.tsx` | ef1b472 + aa2cbd5 |
| VIDEO banner cyclic auto-rotation fix — removed `loop` attr (suppressed `onEnded`, causing banners to never advance); added `setInterval` (8s, `VITE_BANNER_ROTATION_MS`) + `onEnded` dual mechanism. ⛔ NEVER re-add `loop` to `<video>` | `frontend: VideoBanner.tsx` | 08f6182 |
| Complete institution portal test coverage — `BannerControllerIT` 16→20 tests (+TICKER type, +VIDEO type+videoUrl, +PUT HERO→VIDEO, +GET VIDEO in active list); `institution-portal.spec.ts` 22→37 tests, 5→8 suites (+Suite 6 Assignments tab, +Suite 7 Banners tab INSTITUTION_ADMIN, +Suite 8 INSTITUTION_ADMIN role); Suite 1 tab count fixed 8→9; `fetchToken()`/`injectAuthAndGo()` accept optional user param | `center-svc: BannerControllerIT.java`; `frontend: tests/e2e/institution-portal.spec.ts` | b7f137f |
| AdminBannersPage Create Banner modal centering + file upload — (1) Modal off-screen: Framer Motion inline `transform` overrides `-translate-x/y-1/2`; fix: flexbox wrapper `fixed inset-0 flex items-center justify-center pointer-events-none` + `createPortal`. (2) Browse buttons silent: `sr-only` (`position:absolute`) clipped by `overflow-hidden`; fix: `hidden`. Image → `FileReader.readAsDataURL()` → base64 in `imageUrl`. Video → `URL.createObjectURL()` → blob URL in `videoUrl`. ⛔ NEVER use `-translate-x/y-1/2` with Framer Motion. ⛔ NEVER `sr-only` file inputs inside `overflow-hidden` | `frontend: AdminBannersPage.tsx` | a41d586 + 569396b |
| INSTITUTION_ADMIN role — institution-scoped admin (hierarchy: INSTITUTION_ADMIN→CENTER_ADMIN); auth-svc V6 migration, Role enum in 6 services, AuthPrincipal helpers, resolveAccessibleCenters scoped to owned centers, SUPER_ADMIN self-reg blocked | `Role.java` (6 services), `AuthPrincipal.java` (6 services), `CenterService.java`, `BannerService.java`, `AssignmentService.java`, `UserRegistrationService.java`, `GlobalExceptionHandler.java`, `V6__add_institution_admin_role.sql`; `frontend: authStore.ts, AppLayout.tsx, AdminPortalPage.tsx, router.tsx, LoginPage.tsx, RegisterPage.tsx, SettingsPage.tsx` | 7f01e5d |
| Disk space permanent fix — log watchdog in start-all.sh (100 MB cap, 10 min interval), scripts/cleanup.sh (new), .gitignore additions (playwright, PNGs, .venv), performance-svc Kafka log suppression | `scripts/start-all.sh`, `scripts/cleanup.sh`, `.gitignore`, `performance-svc/application.yml` | 7f01e5d |
| Register dropdown fix + district for all roles — SearchableSelect/MultiSelectDropdown: onBlur moved to container div with relatedTarget check (fixes blink-and-close caused by inner autoFocus); District SearchableSelect added to STUDENT (new), PARENT (was text input), TEACHER (was text input), INSTITUTION_ADMIN (new); all data from getDistricts() in indiaLocations.ts; studentDistrict sent in student profile POST | `RegisterPage.tsx` | c75c549 |
| Universal address fields + correct field order — Address Line 1 + Line 2 added to STUDENT, PARENT, TEACHER (was only on INSTITUTION_ADMIN); order standardised: Address L1 → Address L2 → Country → State → District → City → Pincode for all 4 roles; TEACHER gains Pincode; state onChange cascades to clear District + City; student POST sends combined address | `RegisterPage.tsx` | bd6964e |
| Location fields side-by-side grid layout — Country+State in grid-cols-2, District+City in grid-cols-2, for all 4 roles (STUDENT/PARENT/TEACHER/INSTITUTION_ADMIN). Layout-only, no logic changes. | `RegisterPage.tsx` | b46b775 |
| Comprehensive India locations + pincode auto-fill — `india-state-district` npm package (772 districts, 36 states/UTs); real-time pincode → State/District/City via India Post API (free, no auth); First/Last Name hidden until role selected; Teacher centers uses plain axios (fixes redirect-to-login bug); Back button on Step 1 | `indiaLocations.ts`, `RegisterPage.tsx`, `package.json` | bf69e3d |
| INSTITUTION_ADMIN as top self-service role — Banner write RBAC already correct (isSuperAdmin OR isInstitutionAdmin); BannerControllerIT 12→15 tests (+3 INSTITUTION_ADMIN cases); AdminJobsPage: replaced "Centre ID not found" error with CenterPicker (fetches GET /api/v1/centers, card list); AdminAssignmentsTab: replaced raw UUID input with centre select dropdown | `BannerService.java`, `BannerController.java`, `BannerControllerIT.java`, `AdminJobsPage.tsx`, `AdminAssignmentsTab.tsx` | 2b19f4d |
| Profile edit forms for ALL roles — PARENT: Edit Profile (name/phone/gender/address/city/state/pincode → PATCH /api/v1/parents/me); CENTER_ADMIN/INSTITUTION_ADMIN/SUPER_ADMIN: Edit Profile (name → PATCH /api/v1/auth/me); Admin welcome greeting uses full name (not split()[0]); SSE notification badge dedup by id prevents inflation on reconnect | `SettingsPage.tsx`, `AdminDashboardPage.tsx`, `useNotifications.ts` | b72784e |
| Admin profile completion 100% — removed avatarUrl from admin allFields (avatar upload is "coming soon", never saves); both AppLayout.tsx ring and SettingsPage.tsx bar now use [name, email] = 2/2 = 100%; STUDENT(8)/PARENT(9)/TEACHER(8)/ADMIN(2) counts | `AppLayout.tsx`, `SettingsPage.tsx` | dc33618 |
| Staff tab CenterPicker for INSTITUTION_ADMIN — `AdminStaffPage` now shows CenterPicker (card list of accessible centres) when no centerId in JWT; `effectiveCenterId = centerId \|\| selectedCenterId`; removes toast.error guard; all staff queries + StaffCard + CreateStaffModal use effectiveCenterId. Same pattern as AdminJobsPage Fix #70 | `frontend: AdminStaffPage.tsx` | ba70c91 |
| Upload zone click-bubble loop fix — `AdminBulkImportTeachersPage` upload div+onClick+programmatic .click() caused click-bubble loop (file chooser opened then closed). Fix: `<label>` wrapping `<input type="file" className="hidden">`. Also added `centerId?` prop so INSTITUTION_ADMIN gets effectiveCenterId from AdminStaffPage. ⛔ RULE: NEVER use div+onClick+.click() for file uploads. ALWAYS use `<label>` wrapping hidden input. | `frontend: AdminBulkImportTeachersPage.tsx, AdminStaffPage.tsx` | 6b23f51 |
| TestContainers Docker Desktop 4.60+ fix (Fix #83) — Docker Desktop 4.60.1 raised MinAPIVersion to 1.44; shaded docker-java defaulted to /v1.41 → HTTP 400 → "Could not find a valid Docker environment" for ALL IT tests. Fix: `<argLine>-Dapi.version=1.47</argLine>` in maven-failsafe-plugin `<configuration>` in root pom.xml. Also fixed psych-svc application-test.yml: added `spring.datasource.hikari.*` pool settings + `management.metrics.tags.*` overrides. ⛔ NEVER remove argLine from pom.xml or hikari/metrics blocks from application-test.yml. | `pom.xml`, `services/psych-svc/src/test/resources/application-test.yml`, `testcontainers.properties` | 2666427 |
| psych-svc self-service psychometric (Fix #84) — null centerId+batchId allowed in CreatePsychProfileRequest, PsychProfile, PsychProfileService. GlobalExceptionHandler: DataIntegrityViolationException → 409. PsychometricPage.tsx + ParentPsychometricPage.tsx self-service flow. 17 IT tests (12 PsychControllerIT + 5 PsychAssessmentIT) all pass. | `psych-svc: PsychProfile.java, CreatePsychProfileRequest.java, PsychProfileService.java, GlobalExceptionHandler.java, PsychAssessmentIT.java, PsychControllerIT.java`; `frontend: PsychometricPage.tsx, ParentPsychometricPage.tsx` | 2666427 |

Full frozen fix list: `~/.claude/projects/.../memory/frozen-fixes.md` (85+ fixes)

---

## E2E Demo Data (Full Journey — completed 2026-03-20)

All entities created dynamically (timestamp namespace `TS=1773998793`). Full details in memory: `e2e-demo-data.md`.

| Entity | Value |
|---|---|
| **Institute** | NexusEd Demo Academy · centerId `174f6651-418a-40d2-8ecf-c386ff19ac7c` |
| **CENTER_ADMIN** | `ca_1773998793@nexused-demo.edu` / `Demo@2026!` · userId `6deef1e4-65f8-428b-b98a-9c9b3b6e1779` |
| **Batch** | JEE 2026 - Batch A · batchId `3889199a-55c0-4992-8375-109df8648b1d` |
| **Student** | Aryan Kumar · `student_1773998793@nexused-demo.edu` · userId `ee624ff9-8a50-495e-a1c5-8d5bbe8db1b9` |
| **Parent** | Suresh Kumar · `parent_1773998793@nexused-demo.edu` · FATHER→Aryan (ACTIVE) |
| **Exam** | JEE Mathematics Chapter 5 Quiz · examId `18daac77-b595-44ae-9226-8b253e12e832` · PUBLISHED · 5Q · 50 marks · Score: 40/50=80% |
| **Assignment** | Quadratic Equations Practice Set · assignId `c4527cf6-ccd3-4f2e-9b00-9f3300c61369` · PUBLISHED · Score: 18/20 GRADED |

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
