/**
 * E2E tests for the public Job Board (Fix #90).
 *
 * Verifies that STUDENT, PARENT, and TEACHER roles can:
 *   - Navigate to their role-specific job board URL
 *   - See the "Job Board" heading and filter controls
 *   - NOT see any "Post a Job" / admin write controls
 *   - Interact with role and job-type filters
 *   - Clear filters with the Clear button
 *
 * Auth strategy: token injection via page.evaluate() (same as institution-portal.spec.ts).
 * The token is fetched once per suite to minimise auth-svc round-trips.
 *
 * Prerequisites:
 *   - All services running (start-all.sh --no-build)
 *   - CAPTCHA_E2E_BYPASS_TOKEN env var set
 *   - At least one OPEN job posting in the DB (created via the admin portal or API)
 *   - Test accounts exist:
 *       STUDENT  — journey.student@test.com  / Test@12345
 *       PARENT   — ravi.parent@test.com      / Test@12345
 *       TEACHER  — teacher1@test.com         / Test@12345
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';

// ─── Constants ────────────────────────────────────────────────────────────────

const AUTH_SVC_URL = 'http://localhost:8182';

const CAPTCHA_BYPASS = process.env.CAPTCHA_E2E_BYPASS_TOKEN
  ?? 'E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD';

const STUDENT = {
  email:    'journey.student@test.com',
  password: 'Test@12345',
  role:     'STUDENT' as const,
};

const PARENT = {
  email:    'ravi.parent@test.com',
  password: 'Test@12345',
  role:     'PARENT' as const,
};

const TEACHER = {
  email:    'teacher1@test.com',
  password: 'Test@12345',
  role:     'TEACHER' as const,
};

type TestUser = typeof STUDENT | typeof PARENT | typeof TEACHER;

// ─── Auth helpers ─────────────────────────────────────────────────────────────

/** Decode a JWT payload without verifying signature. */
function decodeJwt(token: string): Record<string, unknown> {
  const payload = token.split('.')[1];
  const padded  = payload.replace(/-/g, '+').replace(/_/g, '/');
  return JSON.parse(Buffer.from(padded, 'base64').toString('utf8'));
}

/** Fetch a fresh JWT for the given user via auth-svc. */
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
        userAgent: 'Playwright/job-board',
        deviceId:  'e2e-job-board',
        ipSubnet:  '127.0.0',
      },
    },
  });

  if (!res.ok()) {
    throw new Error(`Login failed ${res.status()} for ${user.email}: ${await res.text()}`);
  }

  const body  = await res.json();
  const token = body.accessToken ?? body.token;
  if (!token) throw new Error(`No token in login response: ${JSON.stringify(body)}`);
  return token as string;
}

/**
 * Inject auth state into localStorage for the given user + token, then navigate.
 * Token is decoded to extract userId / centerId so no hardcoded IDs are needed.
 */
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
    ([t, u, c]: [string, typeof user, Record<string, unknown>]) => {
      localStorage.setItem(
        'edupath-auth',
        JSON.stringify({
          state: {
            token:           t,
            refreshToken:    null,
            deviceId:        'e2e-job-board',
            isAuthenticated: true,
            user: {
              id:       c['sub'],
              email:    u.email,
              role:     c['role'] ?? u.role,
              centerId: c['centerId'] ?? null,
              name:     u.email.split('@')[0],
            },
          },
          version: 0,
        })
      );
    },
    [token, user, claims] as [string, typeof user, Record<string, unknown>],
  );

  await page.goto(url, { waitUntil: 'load', timeout: 30_000 });
}

// ─── Suite 1 — Student Job Board ──────────────────────────────────────────────

test.describe('Suite 1 — Student Job Board', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await fetchToken(request, STUDENT);
  });

  test('renders Job Board heading and filter controls', async ({ page }) => {
    await injectAuthAndGo(page, token, STUDENT, '/jobs');
    await expect(page.getByRole('heading', { name: /job board/i })).toBeVisible();
    await expect(page.getByPlaceholder(/search job title/i)).toBeVisible();
    await expect(page.getByRole('combobox').first()).toBeVisible(); // role filter
  });

  test('does NOT show Post a Job / admin write buttons', async ({ page }) => {
    await injectAuthAndGo(page, token, STUDENT, '/jobs');
    await expect(page.getByRole('button', { name: /post a job/i })).not.toBeVisible();
    await expect(page.getByRole('button', { name: /publish/i })).not.toBeVisible();
  });

  test('nav sidebar contains Job Board link', async ({ page }) => {
    await injectAuthAndGo(page, token, STUDENT, '/jobs');
    await expect(page.getByRole('link', { name: /job board/i })).toBeVisible();
  });

  test('role filter select changes without error', async ({ page }) => {
    await injectAuthAndGo(page, token, STUDENT, '/jobs');
    const roleSelect = page.getByRole('combobox').first();
    await roleSelect.selectOption({ label: 'Teacher' });
    // after selecting, page should not crash — either results or empty state appears
    await expect(
      page.locator('text=/open position|No open positions found/i').first()
    ).toBeVisible({ timeout: 8_000 });
  });

  test('job type filter select works', async ({ page }) => {
    await injectAuthAndGo(page, token, STUDENT, '/jobs');
    const selects = page.getByRole('combobox');
    await selects.nth(1).selectOption({ label: 'Full Time' });
    await expect(
      page.locator('text=/open position|No open positions found/i').first()
    ).toBeVisible({ timeout: 8_000 });
  });

  test('Clear button appears when filters are active and resets them', async ({ page }) => {
    await injectAuthAndGo(page, token, STUDENT, '/jobs');
    const roleSelect = page.getByRole('combobox').first();
    await roleSelect.selectOption({ label: 'Teacher' });
    const clearBtn = page.getByRole('button', { name: /clear/i });
    await expect(clearBtn).toBeVisible();
    await clearBtn.click();
    await expect(roleSelect).toHaveValue('');
  });
});

// ─── Suite 2 — Parent Job Board ───────────────────────────────────────────────

test.describe('Suite 2 — Parent Job Board', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await fetchToken(request, PARENT);
  });

  test('renders Job Board heading at /parent/jobs', async ({ page }) => {
    await injectAuthAndGo(page, token, PARENT, '/parent/jobs');
    await expect(page.getByRole('heading', { name: /job board/i })).toBeVisible();
  });

  test('does NOT show Post a Job button', async ({ page }) => {
    await injectAuthAndGo(page, token, PARENT, '/parent/jobs');
    await expect(page.getByRole('button', { name: /post a job/i })).not.toBeVisible();
  });

  test('nav sidebar contains Job Board link', async ({ page }) => {
    await injectAuthAndGo(page, token, PARENT, '/parent/jobs');
    await expect(page.getByRole('link', { name: /job board/i })).toBeVisible();
  });

  test('city filter input accepts text input', async ({ page }) => {
    await injectAuthAndGo(page, token, PARENT, '/parent/jobs');
    const cityInput = page.getByPlaceholder('City…');
    await cityInput.fill('Bengaluru');
    await expect(
      page.locator('text=/open position|No open positions found/i').first()
    ).toBeVisible({ timeout: 8_000 });
  });
});

// ─── Suite 3 — Teacher Job Board ──────────────────────────────────────────────

test.describe('Suite 3 — Teacher Job Board', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await fetchToken(request, TEACHER);
  });

  test('renders Job Board heading at /mentor-portal/jobs', async ({ page }) => {
    await injectAuthAndGo(page, token, TEACHER, '/mentor-portal/jobs');
    await expect(page.getByRole('heading', { name: /job board/i })).toBeVisible();
  });

  test('does NOT show Post a Job button', async ({ page }) => {
    await injectAuthAndGo(page, token, TEACHER, '/mentor-portal/jobs');
    await expect(page.getByRole('button', { name: /post a job/i })).not.toBeVisible();
  });

  test('nav sidebar contains Job Board link', async ({ page }) => {
    await injectAuthAndGo(page, token, TEACHER, '/mentor-portal/jobs');
    await expect(page.getByRole('link', { name: /job board/i })).toBeVisible();
  });

  test('search input accepts text and triggers debounced query', async ({ page }) => {
    await injectAuthAndGo(page, token, TEACHER, '/mentor-portal/jobs');
    const searchInput = page.getByPlaceholder(/search job title/i);
    await searchInput.fill('teacher');
    // Debounce is 400ms — wait for results to settle
    await page.waitForTimeout(600);
    await expect(
      page.locator('text=/open position|No open positions found/i').first()
    ).toBeVisible({ timeout: 8_000 });
    // Clear button (X) should appear in search box
    await expect(page.locator('input[placeholder*="Search"] ~ button').first()).toBeVisible();
  });
});
