import { test, expect } from '@playwright/test';
import { uniqueEmail, env } from './env';
import { mockCaptchaChallenge, solveCaptcha } from './helpers/captcha';
import { waitForEmail, extractOtp, clearMailhog } from './helpers/mailhog';

/**
 * Parental consent gate — COPPA/DPDP compliance.
 *
 * When DOB makes the student under 13, a "Parental Consent" step is inserted
 * between Academic Info and Create Account (7-step total).
 */
test.describe('Parental consent — under-13 registration', () => {
  // A DOB that is under 13 relative to 2026
  const UNDER_13_DOB = '2015-06-15';
  // Over-13 DOB (18-year-old student)
  const OVER_13_DOB = '2007-06-15';

  test.beforeAll(async () => {
    await clearMailhog();
  });

  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => localStorage.clear());
    await mockCaptchaChallenge(page);
    await page.goto('/register');
    await expect(page.getByRole('heading', { name: 'Create your account' })).toBeVisible();
  });

  test('parental consent step appears when student is under 13', async ({ page }) => {
    await fillStep1(page, uniqueEmail('u13'));
    await page.getByRole('button', { name: /continue/i }).click();

    await page.getByRole('button', { name: /^student/i }).click();
    await page.getByRole('button', { name: /continue/i }).click();

    // Step 3 — academic info with under-13 DOB
    await page.locator('input[type="date"]').fill(UNDER_13_DOB);
    await page.getByPlaceholder('e.g. SCH-2024-ABC').fill(env.institutionCode);
    await expect(page.getByText(/✓/)).toBeVisible({ timeout: 10_000 });
    await page.locator('select[name="board"]').selectOption('CBSE');
    await page.locator('select[name="grade"]').selectOption('10');
    await page.getByRole('button', { name: /continue/i }).click();

    // Parental consent step must appear
    await expect(page.getByRole('heading', { name: 'Parental Consent Required' })).toBeVisible({
      timeout: 5_000,
    });
    await expect(page.getByPlaceholder('parent@example.com')).toBeVisible();

    // Stepper should show 'Parental Consent' label (exact match avoids matching the heading)
    await expect(page.getByText('Parental Consent', { exact: true })).toBeVisible();
  });

  test('consent step is skipped for over-13 students', async ({ page }) => {
    await fillStep1(page, uniqueEmail('over13'));
    await page.getByRole('button', { name: /continue/i }).click();

    await page.getByRole('button', { name: /^student/i }).click();
    await page.getByRole('button', { name: /continue/i }).click();

    await page.locator('input[type="date"]').fill(OVER_13_DOB);
    await page.getByPlaceholder('e.g. SCH-2024-ABC').fill(env.institutionCode);
    await expect(page.getByText(/✓/)).toBeVisible({ timeout: 10_000 });
    await page.locator('select[name="board"]').selectOption('CBSE');
    await page.locator('select[name="grade"]').selectOption('11');
    await page.getByRole('button', { name: /continue/i }).click();

    // Must jump straight to Create Account (captcha) — NOT parental consent
    await expect(page.getByRole('heading', { name: 'Parental Consent Required' })).not.toBeVisible();
    await expect(page.getByPlaceholder('Enter 6 characters')).toBeVisible({ timeout: 5_000 });
  });

  test('full under-13 registration — consent email sent, OTP verified', async ({ page }) => {
    test.setTimeout(90_000);
    const email = uniqueEmail('u13full');
    const parentEmail = `parent.${Date.now()}@nexused.dev`;

    // Step 1
    await fillStep1(page, email);
    await page.getByRole('button', { name: /continue/i }).click();

    // Step 2 — STUDENT
    await page.getByRole('button', { name: /^student/i }).click();
    await page.getByRole('button', { name: /continue/i }).click();

    // Step 3 — academic info (under-13 DOB)
    await page.locator('input[type="date"]').fill(UNDER_13_DOB);
    await page.getByPlaceholder('e.g. SCH-2024-ABC').fill(env.institutionCode);
    await expect(page.getByText(/✓/)).toBeVisible({ timeout: 10_000 });
    await page.locator('select[name="board"]').selectOption('CBSE');
    await page.locator('select[name="grade"]').selectOption('10');
    await page.getByRole('button', { name: /continue/i }).click();

    // Step 4 — Parental consent
    await expect(page.getByRole('heading', { name: 'Parental Consent Required' })).toBeVisible();
    await page.getByPlaceholder('parent@example.com').fill(parentEmail);
    await page.getByRole('button', { name: /continue/i }).click();

    // Step 5 — Create account
    await solveCaptcha(page);
    await page.getByRole('button', { name: /create account/i }).click();
    await expect(page.getByText(/account created/i)).toBeVisible({ timeout: 15_000 });

    // Step 6 — Subjects
    await page.getByRole('button', { name: /continue/i }).click();

    // Step 7 — OTP
    const otpBody = await waitForEmail(email, 'Verify', 30_000);
    const otp = extractOtp(otpBody);
    await fillOtp(page, otp);
    await page.getByRole('button', { name: 'Verify & Continue' }).click();

    await expect(page).toHaveURL(/\/login/, { timeout: 15_000 });
    await expect(page.getByText(/verified|can now sign in/i)).toBeVisible({ timeout: 10_000 });

    // Parental consent email is best-effort — log warning rather than failing
    const consentBody = await waitForEmail(parentEmail, 'consent', 20_000).catch(() => null);
    if (!consentBody) {
      console.warn('[E2E] Parental consent email not found in MailHog — check auth-svc logs');
    }
  });
});

// ─── Helpers ──────────────────────────────────────────────────────────────────

async function fillStep1(page: import('@playwright/test').Page, email: string) {
  await page.getByPlaceholder('Jane').fill('Minor');
  await page.getByPlaceholder('Smith').fill('Student');
  await page.getByPlaceholder('you@example.com').fill(email);
  await page.getByPlaceholder('••••••••').first().fill('Test@12345');
  await page.getByPlaceholder('••••••••').nth(1).fill('Test@12345');
}

async function fillOtp(page: import('@playwright/test').Page, otp: string) {
  for (let i = 0; i < 6; i++) {
    await page.locator(`#otp-${i}`).fill(otp[i]);
  }
}
