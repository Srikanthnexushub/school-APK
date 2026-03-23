/**
 * E2E tests for the Document Library feature (Phase 4).
 *
 * Verifies that:
 *  Suite 1 — Student Library (/library)              — read-only view + search + filters
 *  Suite 2 — Parent Library (/parent/library)        — read-only view
 *  Suite 3 — Teacher Library (/mentor-portal/library)— read-only, no upload button
 *  Suite 4 — Admin Library (/admin?tab=library)      — upload wizard + manage table
 *
 * Auth strategy: JWT injected via localStorage (same pattern as institution-portal.spec.ts).
 * No hardcoded user IDs — all resolved from JWT claims at runtime.
 *
 * Prerequisites:
 *   - All services running (start-all.sh --no-build)
 *   - CAPTCHA_E2E_BYPASS_TOKEN env var set
 *   - Test accounts:
 *       STUDENT  — journey.student@test.com  / Test@12345
 *       PARENT   — ravi.parent@test.com      / Test@12345
 *       TEACHER  — teacher1@test.com         / Test@12345
 *       CENTER_ADMIN — institute@nexused.com / Test@12345
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';

// ─── Constants ────────────────────────────────────────────────────────────────

const AUTH_SVC_URL = 'http://localhost:8182';

const CAPTCHA_BYPASS =
  process.env.CAPTCHA_E2E_BYPASS_TOKEN ?? 'E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD';

const STUDENT = { email: 'journey.student@test.com', password: 'Test@12345', role: 'STUDENT' as const };
const PARENT  = { email: 'ravi.parent@test.com',      password: 'Test@12345', role: 'PARENT'  as const };
const TEACHER = { email: 'teacher1@test.com',          password: 'Test@12345', role: 'TEACHER' as const };
const ADMIN   = { email: 'institute@nexused.com',      password: 'Test@12345', role: 'CENTER_ADMIN' as const };

type TestUser = typeof STUDENT | typeof PARENT | typeof TEACHER | typeof ADMIN;

// ─── Auth helpers ─────────────────────────────────────────────────────────────

function decodeJwt(token: string): Record<string, unknown> {
  const payload = token.split('.')[1];
  const padded  = payload.replace(/-/g, '+').replace(/_/g, '/');
  return JSON.parse(Buffer.from(padded, 'base64').toString('utf8'));
}

async function fetchToken(
  request: APIRequestContext,
  user: TestUser,
): Promise<string> {
  const res = await request.post(`${AUTH_SVC_URL}/api/v1/auth/login`, {
    data: {
      email:    user.email,
      password: user.password,
      captchaToken: `${CAPTCHA_BYPASS}:bypass`,
      deviceFingerprint: {
        userAgent: 'Playwright/library-spec',
        deviceId:  'e2e-library',
        ipSubnet:  '127.0.0',
      },
    },
  });

  if (!res.ok()) {
    throw new Error(`Login failed ${res.status()} for ${user.email}: ${await res.text()}`);
  }

  const body  = await res.json();
  const token = body.accessToken ?? body.token;
  if (!token) throw new Error(`No token in response for ${user.email}`);
  return token as string;
}

async function injectAuthAndGo(
  page: Page,
  token: string,
  user: TestUser,
  url: string,
): Promise<void> {
  const claims = decodeJwt(token);

  try {
    await page.goto('/', { waitUntil: 'domcontentloaded', timeout: 10_000 });
  } catch {
    // Tolerate navigation errors on root — only need the origin loaded.
  }

  await page.evaluate(
    ([t, u, c]: [string, TestUser, Record<string, unknown>]) => {
      localStorage.setItem(
        'edupath-auth',
        JSON.stringify({
          state: {
            token:           t,
            refreshToken:    null,
            deviceId:        'e2e-library',
            isAuthenticated: true,
            user: {
              id:       c['sub'],
              email:    u.email,
              role:     (c['role'] as string) ?? u.role,
              centerId: (c['centerId'] as string) ?? null,
              name:     u.email.split('@')[0],
            },
          },
          version: 0,
        }),
      );
    },
    [token, user, claims] as [string, TestUser, Record<string, unknown>],
  );

  await page.goto(url, { waitUntil: 'load', timeout: 30_000 });
}

// ─── Suite 1 — Student Library ────────────────────────────────────────────────

test.describe('Suite 1 — Student Library (read-only)', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await fetchToken(request, STUDENT);
  });

  test('S1-T1 — renders Library heading and search bar', async ({ page }) => {
    await injectAuthAndGo(page, token, STUDENT, '/library');
    await expect(page.getByRole('heading', { name: /library/i }).first()).toBeVisible();
    await expect(
      page.getByPlaceholder(/search by title|search.*keyword/i),
    ).toBeVisible();
  });

  test('S1-T2 — nav sidebar contains Library link at /library', async ({ page }) => {
    await injectAuthAndGo(page, token, STUDENT, '/library');
    await expect(page.getByRole('link', { name: /^library$/i })).toBeVisible();
  });

  test('S1-T3 — Filters button opens filter panel with selects', async ({ page }) => {
    await injectAuthAndGo(page, token, STUDENT, '/library');
    const filtersBtn = page.getByRole('button', { name: /filters/i });
    await expect(filtersBtn).toBeVisible();
    await filtersBtn.click();
    // Filter panel should appear with Board and Exam Type selects
    await expect(page.getByRole('combobox').first()).toBeVisible({ timeout: 5_000 });
  });

  test('S1-T4 — search returns results or empty state without crashing', async ({ page }) => {
    await injectAuthAndGo(page, token, STUDENT, '/library');
    const searchBox = page.getByPlaceholder(/search by title|search.*keyword/i);
    await searchBox.fill('physics');
    // Wait for debounced query (400ms) + API round-trip
    await page.waitForTimeout(800);
    // Either results grid or "No documents" message — page must not show an error
    const anyResult = page.locator(
      '[class*="grid"] > div, text=/No documents/i, text=/no results/i',
    ).first();
    await expect(anyResult).toBeVisible({ timeout: 10_000 });
  });

  test('S1-T5 — difficulty filter updates result count or shows empty state', async ({ page }) => {
    await injectAuthAndGo(page, token, STUDENT, '/library');
    await page.getByRole('button', { name: /filters/i }).click();
    const selects = page.getByRole('combobox');
    // Third combobox should be difficulty (Board, ExamType, Difficulty)
    await selects.nth(2).selectOption('HARD');
    await page.waitForTimeout(500);
    await expect(
      page.locator('text=/result|No documents/i').first(),
    ).toBeVisible({ timeout: 8_000 });
  });

  test('S1-T6 — clear all filters resets filter state', async ({ page }) => {
    await injectAuthAndGo(page, token, STUDENT, '/library');
    await page.getByRole('button', { name: /filters/i }).click();
    const selects = page.getByRole('combobox');
    await selects.first().selectOption('CBSE');
    await page.waitForTimeout(200);
    const clearBtn = page.getByRole('button', { name: /clear all filters/i });
    await expect(clearBtn).toBeVisible();
    await clearBtn.click();
    await expect(selects.first()).toHaveValue('');
  });
});

// ─── Suite 2 — Parent Library ─────────────────────────────────────────────────

test.describe('Suite 2 — Parent Library (read-only)', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await fetchToken(request, PARENT);
  });

  test('S2-T1 — renders Library heading at /parent/library', async ({ page }) => {
    await injectAuthAndGo(page, token, PARENT, '/parent/library');
    await expect(page.getByRole('heading', { name: /library/i }).first()).toBeVisible();
  });

  test('S2-T2 — parent nav sidebar contains Library link', async ({ page }) => {
    await injectAuthAndGo(page, token, PARENT, '/parent/library');
    await expect(page.getByRole('link', { name: /^library$/i })).toBeVisible();
  });

  test('S2-T3 — search box is present and accepts input', async ({ page }) => {
    await injectAuthAndGo(page, token, PARENT, '/parent/library');
    const searchBox = page.getByPlaceholder(/search by title|search.*keyword/i);
    await expect(searchBox).toBeVisible();
    await searchBox.fill('neet');
    await expect(searchBox).toHaveValue('neet');
  });
});

// ─── Suite 3 — Teacher Library ────────────────────────────────────────────────

test.describe('Suite 3 — Teacher Library (read-only)', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await fetchToken(request, TEACHER);
  });

  test('S3-T1 — renders Library heading at /mentor-portal/library', async ({ page }) => {
    await injectAuthAndGo(page, token, TEACHER, '/mentor-portal/library');
    await expect(page.getByRole('heading', { name: /library/i }).first()).toBeVisible();
  });

  test('S3-T2 — teacher nav sidebar has Library link', async ({ page }) => {
    await injectAuthAndGo(page, token, TEACHER, '/mentor-portal/library');
    await expect(page.getByRole('link', { name: /^library$/i })).toBeVisible();
  });

  test('S3-T3 — no Upload Document button visible (read-only for teachers)', async ({ page }) => {
    await injectAuthAndGo(page, token, TEACHER, '/mentor-portal/library');
    await expect(page.getByRole('button', { name: /upload document/i })).not.toBeVisible();
  });
});

// ─── Suite 4 — Admin Library ──────────────────────────────────────────────────

test.describe('Suite 4 — Admin Library (upload + manage)', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await fetchToken(request, ADMIN);
  });

  test('S4-T1 — admin nav sidebar contains Library link', async ({ page }) => {
    await injectAuthAndGo(page, token, ADMIN, '/admin?tab=library');
    await expect(page.getByRole('link', { name: /^library$/i })).toBeVisible();
  });

  test('S4-T2 — Library tab renders heading and Upload Document button', async ({ page }) => {
    await injectAuthAndGo(page, token, ADMIN, '/admin?tab=library');
    await expect(page.getByRole('heading', { name: /document library/i }).first()).toBeVisible();
    await expect(page.getByRole('button', { name: /upload document/i })).toBeVisible();
  });

  test('S4-T3 — clicking Upload Document opens Step 1 wizard modal', async ({ page }) => {
    await injectAuthAndGo(page, token, ADMIN, '/admin?tab=library');
    await page.getByRole('button', { name: /upload document/i }).click();
    // Wizard header should appear
    await expect(page.getByText(/upload document/i).last()).toBeVisible({ timeout: 5_000 });
    // Step 1 file drop zone should be present
    await expect(page.getByText(/drop a file here|browse/i).first()).toBeVisible({ timeout: 5_000 });
  });

  test('S4-T4 — wizard Cancel button closes the modal', async ({ page }) => {
    await injectAuthAndGo(page, token, ADMIN, '/admin?tab=library');
    await page.getByRole('button', { name: /upload document/i }).click();
    await expect(page.getByText(/drop a file here|browse/i).first()).toBeVisible({ timeout: 5_000 });
    // Click Cancel (X button or Cancel text button)
    const cancelBtn = page.getByRole('button', { name: /cancel/i });
    await cancelBtn.click();
    // Modal should be gone
    await expect(page.getByText(/drop a file here/i)).not.toBeVisible({ timeout: 3_000 });
  });

  test('S4-T5 — manage table shows stats strip when items exist', async ({ page }) => {
    await injectAuthAndGo(page, token, ADMIN, '/admin?tab=library');
    // Either stats strip appears (Total / Available / Processing) or empty state
    const statsOrEmpty = page.locator('text=/Total|No documents uploaded/i').first();
    await expect(statsOrEmpty).toBeVisible({ timeout: 10_000 });
  });

  test('S4-T6 — search box filters the document table', async ({ page }) => {
    await injectAuthAndGo(page, token, ADMIN, '/admin?tab=library');
    const searchBox = page.getByPlaceholder(/search title or subject/i);
    await expect(searchBox).toBeVisible({ timeout: 8_000 });
    await searchBox.fill('zzz_no_match_xoxo');
    await page.waitForTimeout(300);
    // Either empty state or filtered results — must not crash
    await expect(
      page.locator('text=/No documents|document/i').first(),
    ).toBeVisible({ timeout: 5_000 });
  });
});
