# Session: Parent Portal Fixes — 2026-03-11

## Fixes Delivered

### 1. Parent Copilot — Child-Name Hallucination (commits f6ca86d, d0a1311)

**Symptom:** Copilot responded with completely fabricated child details ("John Doe, 15-year-old,
INTJ, Delhi Public School") regardless of which parent was logged in.

**Root cause — three independent failures:**

| Layer | Bug | Fix |
|---|---|---|
| Frontend | `linkedStudents` query did `r.data` directly on a Spring `Page<T>` object → got a plain object not an array → `defaultStudentId` was always `null` → `studentId: ''` sent to backend | Apply Page extraction: `const d = r.data; return Array.isArray(d) ? d : (d.content ?? [])` |
| Backend | `CopilotService.resolveStudentName()` looked up `student_links` by JWT **user ID** (`30a06234-4e3d-4fa0-91a3-7705646c2b21`), but `student_links.parent_id` stores the **profile ID** (`59ca5bc7-...`) | Resolve via `parentProfileRepository.findByUserId(userUuid)` first, then use `profile.getId()` |
| Backend | `ParentProfileRepository` was not injected into `CopilotService` | Added as constructor dependency |

**All three fixes are FROZEN — never revert (commit d0a1311).**

**Verification:** "Who is my child?" → "Your child is **Test Student**." + real psychometric scores
(Openness 90/100, Conscientiousness 90/100, Neuroticism 40/100, RIASEC I-A-S).

**Files changed:**
- `frontend/web/src/pages/parent/ParentCopilotPage.tsx` — line ~235, linkedStudents queryFn
- `services/parent-svc/src/main/java/com/edutech/parent/application/service/CopilotService.java`
  - `resolveStudentName()` — profile ID chain
  - Constructor — added `ParentProfileRepository` parameter + field

---

### 2. Parent Dashboard — Fees Card Live Data (commit 359eccf)

**Symptom:** The "Outstanding Fee" card on the parent dashboard:
- Showed no amount (hardcoded/empty)
- "Pay Now" button triggered a fake toast ("Redirecting to payment gateway…") instead of navigating

**Fix:**
- Added `outstandingAmount` = sum of `amountPaid` for payments where `status === 'PENDING'` and
  `studentId === resolvedStudentId`, computed from the live `feePayments` API response
- Rendered `₹{outstandingAmount.toLocaleString('en-IN')}` in bold red when `> 0`
- Changed "Pay Now" `onClick` to `navigate('/parent/fees')`

**Verification:** Screenshot confirmed ₹35,000 displayed in red with working navigation.

**File changed:**
- `frontend/web/src/pages/parent/ParentDashboardPage.tsx`

---

## Key Patterns Reinforced

- **Page<> extraction is universal** — every Spring-paginated endpoint returns `{ content: [], totalElements, ... }` not a plain array. All frontend queries must use the extraction pattern.
- **JWT user ID ≠ profile ID** — auth-svc stores users by `user_id` UUID; parent-svc stores profile by its own `id` UUID. Never assume they are the same.
- **Local-echo mode extracts from system prompt** — `AnthropicWebClientAdapter.executeLocalEcho()` and `OpenRouterWebClientAdapter.executeLocalEcho()` parse the system prompt for "linked child is: NAME" and Big Five scores to return factual responses in dev mode.
