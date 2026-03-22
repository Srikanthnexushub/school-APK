/**
 * E2E tests: Student onboarding — zero-state flow.
 *
 * Covers the critical path for a BRAND-NEW student who has never had a profile
 * row in student_profile_db.  This was the source of two silent regressions:
 *
 *   Fix #86 — SettingsPage saveMutation always called PATCH /api/v1/students/me,
 *              which returned 404 for new students with no profile row.
 *              Fix: detect !profile → call POST /api/v1/students (create).
 *
 *   Fix #87 — AppLayout profile-completion ring stayed at 0% for new students
 *              because the `&& studentProfile` guard short-circuited the
 *              percentage calculation when studentProfile was undefined.
 *              Fix: remove guard, use optional chaining; new student with
 *              name+email gets 2/8 = 25%.
 *
 * Flow tested here:
 *   1. Register a fresh student via auth-svc API (no UI captcha needed).
 *   2. Poll MailHog for the OTP and verify via API.
 *   3. Inject the resulting JWT into localStorage (avoids live captcha overhead).
 *   4. Navigate to /settings (defaults to Profile tab).
 *   5. Fill and save the profile form.
 *   6. Assert: success toast visible, no error toast, ring shows > 0%.
 *
 * Prerequisites:
 *   - All services running:  bash scripts/start-all.sh --no-build
 *   - CAPTCHA_E2E_BYPASS_TOKEN env var set (matches auth-svc config)
 *   - MailHog reachable at http://localhost:8025
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { waitForEmail, extractOtp, clearMailhog } from './helpers/mailhog';

// ─── Constants ────────────────────────────────────────────────────────────────

const AUTH_SVC_URL  = 'http://localhost:8182';
const CAPTCHA_BYPASS = process.env.CAPTCHA_E2E_BYPASS_TOKEN
  ?? 'E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD';

const DEVICE: object = {
  userAgent: 'Playwright/student-onboarding',
  deviceId:  'e2e-student-onboarding',
  ipSubnet:  '127.0.0',
};

/** Generate a unique email to avoid duplicate-account errors between runs. */
function freshEmail(): string {
  return `onboard.${Date.now()}@nexused.dev`;
}

// ─── API helpers ──────────────────────────────────────────────────────────────

/** Register a fresh student via auth-svc API.  Returns the email used. */
async function registerStudent(
  request: APIRequestContext,
  email: string,
): Promise<void> {
  const res = await request.post(`${AUTH_SVC_URL}/api/v1/auth/register`, {
    data: {
      email,
      password:          'Test@12345',
      role:              'STUDENT',
      centerId:          null,
      firstName:         'Onboard',
      lastName:          'Test',
      captchaToken:      `${CAPTCHA_BYPASS}:bypass`,
      deviceFingerprint: DEVICE,
    },
  });

  if (!res.ok()) {
    throw new Error(`Registration failed ${res.status()}: ${await res.text()}`);
  }
}

/**
 * Verify OTP for the given email via auth-svc API.
 * Returns the access token from the verify response.
 */
async function verifyOtpAndGetToken(
  request: APIRequestContext,
  email: string,
  otp: string,
): Promise<string> {
  const res = await request.post(`${AUTH_SVC_URL}/api/v1/otp/verify`, {
    data: { email, otp, purpose: 'EMAIL_VERIFICATION' },
  });

  // Verify returns 200 with a TokenPair when activation succeeds,
  // or 204 No Content when OTP is accepted but no token is issued.
  if (!res.ok()) {
    throw new Error(`OTP verify failed ${res.status()}: ${await res.text()}`);
  }

  if (res.status() === 204) {
    // No token returned on verify — need to login separately
    const loginRes = await request.post(`${AUTH_SVC_URL}/api/v1/auth/login`, {
      data: {
        email,
        password:          'Test@12345',
        captchaToken:      `${CAPTCHA_BYPASS}:bypass`,
        deviceFingerprint: DEVICE,
      },
    });
    if (!loginRes.ok()) {
      throw new Error(`Login after verify failed ${loginRes.status()}: ${await loginRes.text()}`);
    }
    const body = await loginRes.json();
    return body.accessToken ?? body.token;
  }

  const body = await res.json();
  const token = body.accessToken ?? body.token;
  if (!token) {
    throw new Error(`No token in OTP verify response: ${JSON.stringify(body)}`);
  }
  return token;
}

/** Decode the JWT payload (base64url) — no crypto needed, we just need sub/role. */
function decodeJwt(token: string): { sub: string; role: string } {
  const payload = token.split('.')[1];
  // base64url → base64 → JSON
  const json = Buffer.from(
    payload.replace(/-/g, '+').replace(/_/g, '/'),
    'base64'
  ).toString('utf-8');
  return JSON.parse(json);
}

/**
 * Inject auth state for a student user into localStorage, then navigate to url.
 * The `name` field mirrors what auth-svc puts in the token (firstName + lastName).
 */
async function injectAuthAndGo(
  page: Page,
  token: string,
  email: string,
  url = '/settings',
): Promise<void> {
  // Navigate to root first so localStorage is scoped to the correct origin.
  try {
    await page.goto('/', { waitUntil: 'domcontentloaded', timeout: 10_000 });
  } catch {
    // Tolerate errors on '/' — we only need the origin loaded.
  }

  const claims = decodeJwt(token);

  await page.evaluate(
    ([t, userId, userEmail]: [string, string, string]) => {
      localStorage.setItem(
        'edupath-auth',
        JSON.stringify({
          state: {
            token:           t,
            refreshToken:    null,
            deviceId:        'e2e-student-onboarding',
            isAuthenticated: true,   // CRITICAL — ProtectedRoute checks this
            user: {
              id:       userId,
              email:    userEmail,
              role:     'STUDENT',
              centerId: null,
              name:     'Onboard Test',
            },
          },
          version: 0,
        })
      );
    },
    [token, claims.sub, email] as [string, string, string],
  );

  await page.goto(url, { waitUntil: 'load', timeout: 30_000 });
}

// ─── Suite 1 — Full zero-state student onboarding ────────────────────────────

test.describe('Suite 1 — New student zero-state profile flow (Fix #86 + #87)', () => {
  test.setTimeout(120_000);

  let token: string;
  let studentEmail: string;

  test.beforeAll(async ({ request }) => {
    // Use a single email for all tests in this suite.
    studentEmail = freshEmail();

    // 1. Clear old MailHog messages so we don't pick up a stale OTP.
    await clearMailhog();

    // 2. Register the brand-new student.
    await registerStudent(request, studentEmail);

    // 3. Wait for OTP email and extract the code.
    const emailBody = await waitForEmail(studentEmail, 'Verify', 45_000);
    const otp = extractOtp(emailBody);

    // 4. Verify OTP → get JWT.
    token = await verifyOtpAndGetToken(request, studentEmail, otp);
  });

  test('Settings profile tab renders without crashing for a zero-state student', async ({ page }) => {
    await injectAuthAndGo(page, token, studentEmail, '/settings');

    // The Profile tab is the default — its heading should be visible.
    await expect(
      page.getByRole('heading', { name: /profile/i }).first()
    ).toBeVisible({ timeout: 15_000 });

    // No error banner or "Something went wrong" should appear.
    await expect(page.getByText(/something went wrong/i)).not.toBeVisible();
    await expect(page.getByText(/failed to load/i)).not.toBeVisible();
  });

  test('saving profile for the first time calls POST (create), not PATCH (Fix #86)', async ({ page }) => {
    await injectAuthAndGo(page, token, studentEmail, '/settings');

    // Wait for the Profile tab form to render.
    await page.waitForSelector('input[placeholder*="Jane"], input[placeholder*="name"], input[name="name"]', {
      timeout: 15_000,
    });

    // Intercept API calls to confirm POST /api/v1/students is called (not PATCH).
    let createCalled = false;
    let patchCalled  = false;

    page.on('request', (req) => {
      if (req.url().includes('/api/v1/students') && !req.url().includes('/me')) {
        if (req.method() === 'POST') createCalled = true;
      }
      if (req.url().includes('/api/v1/students/me') && req.method() === 'PATCH') {
        patchCalled = true;
      }
    });

    // Fill the name field (other fields are optional for the create call).
    const nameInput = page.locator('input').filter({ hasText: '' }).first();
    // Try common name field selectors
    const nameField = page
      .locator('input[placeholder*="Jane Smith"], input[name="name"], input[id*="name"]')
      .first();
    if (await nameField.isVisible({ timeout: 3_000 }).catch(() => false)) {
      await nameField.clear();
      await nameField.fill('Onboard Test');
    }

    // Click the Save button.
    await page.getByRole('button', { name: /save/i }).first().click();

    // Success toast must appear.
    await expect(
      page.getByText(/profile updated successfully/i)
    ).toBeVisible({ timeout: 15_000 });

    // POST (create) must have been triggered, PATCH must NOT have been.
    expect(createCalled).toBe(true);
    expect(patchCalled).toBe(false);
  });

  test('profile completion ring shows > 0% for a new student (Fix #87)', async ({ page }) => {
    await injectAuthAndGo(page, token, studentEmail, '/settings');

    // Wait for the layout ring to render.
    // The ring is in AppLayout — look for the percentage text in the sidebar.
    await page.waitForSelector('[data-testid="profile-ring"], .profile-ring, [aria-label*="profile"]', {
      timeout: 10_000,
    }).catch(() => {
      // If no testid — fall back to looking for the % text anywhere in the header/sidebar.
    });

    // A new student with name + email already filled in auth store should show > 0%.
    // Look for any text matching a percentage that is NOT 0%.
    // The ring SVG text is something like "25%".
    const pctLocator = page.locator('text=/^\\d+%$/').first();
    await expect(pctLocator).toBeVisible({ timeout: 15_000 });

    const pctText = await pctLocator.textContent();
    const pct = parseInt(pctText?.replace('%', '') ?? '0', 10);
    expect(pct).toBeGreaterThan(0);
  });

  test('second save on existing profile calls PATCH (update), not POST (Fix #86)', async ({ page }) => {
    // After the previous test, the student profile row now exists.
    // Navigate to /settings again — this time profile query returns 200 data.
    await injectAuthAndGo(page, token, studentEmail, '/settings');

    await page.waitForSelector('input', { timeout: 15_000 });

    let createCalled = false;
    let patchCalled  = false;

    page.on('request', (req) => {
      if (req.url().includes('/api/v1/students') && !req.url().includes('/me')) {
        if (req.method() === 'POST') createCalled = true;
      }
      if (req.url().includes('/api/v1/students/me') && req.method() === 'PATCH') {
        patchCalled = true;
      }
    });

    // Click Save without changing anything.
    await page.getByRole('button', { name: /save/i }).first().click();

    await expect(
      page.getByText(/profile updated successfully/i)
    ).toBeVisible({ timeout: 15_000 });

    // Now PATCH must be called (profile exists), POST must NOT.
    expect(patchCalled).toBe(true);
    expect(createCalled).toBe(false);
  });
});

// ─── Suite 2 — Regression guard: existing student unchanged ──────────────────

test.describe('Suite 2 — Existing student profile save still works', () => {
  test.setTimeout(60_000);

  const EXISTING_STUDENT = {
    email:    'journey.student@test.com',
    password: 'Test@12345',
    role:     'STUDENT' as const,
  };

  let token: string;

  test.beforeAll(async ({ request }) => {
    const res = await request.post(`${AUTH_SVC_URL}/api/v1/auth/login`, {
      data: {
        email:             EXISTING_STUDENT.email,
        password:          EXISTING_STUDENT.password,
        captchaToken:      `${CAPTCHA_BYPASS}:bypass`,
        deviceFingerprint: DEVICE,
      },
    });
    if (!res.ok()) {
      // Fallback to the other known student account
      const res2 = await request.post(`${AUTH_SVC_URL}/api/v1/auth/login`, {
        data: {
          email:             'qa-test@nexused.dev',
          password:          'Test@12345',
          captchaToken:      `${CAPTCHA_BYPASS}:bypass`,
          deviceFingerprint: DEVICE,
        },
      });
      if (!res2.ok()) throw new Error('Could not login as any known student');
      token = (await res2.json()).accessToken;
    } else {
      token = (await res.json()).accessToken;
    }
  });

  test('existing student profile save calls PATCH (no regression)', async ({ page }) => {
    const claims = decodeJwt(token);

    try {
      await page.goto('/', { waitUntil: 'domcontentloaded', timeout: 10_000 });
    } catch { /* tolerate */ }

    await page.evaluate(
      ([t, userId]: [string, string]) => {
        localStorage.setItem('edupath-auth', JSON.stringify({
          state: {
            token: t, refreshToken: null,
            deviceId: 'e2e-student-existing',
            isAuthenticated: true,
            user: { id: userId, email: 'journey.student@test.com', role: 'STUDENT', centerId: null, name: 'Journey Student' },
          },
          version: 0,
        }));
      },
      [token, claims.sub] as [string, string],
    );

    await page.goto('/settings', { waitUntil: 'load', timeout: 30_000 });
    await page.waitForSelector('input', { timeout: 15_000 });

    let patchCalled = false;
    page.on('request', (req) => {
      if (req.url().includes('/api/v1/students/me') && req.method() === 'PATCH') {
        patchCalled = true;
      }
    });

    await page.getByRole('button', { name: /save/i }).first().click();

    await expect(
      page.getByText(/profile updated successfully/i)
    ).toBeVisible({ timeout: 15_000 });

    expect(patchCalled).toBe(true);
  });
});
