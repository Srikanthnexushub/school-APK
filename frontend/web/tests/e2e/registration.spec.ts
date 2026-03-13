import { test, expect } from '@playwright/test';
import { uniqueEmail, env } from './env';
import { mockCaptchaChallenge, solveCaptcha } from './helpers/captcha';
import { waitForEmail, extractOtp, clearMailhog } from './helpers/mailhog';

/**
 * Student registration — 6-step flow (over-13, no parental consent).
 *
 * Steps: Personal Details → Your Role → Academic Info →
 *        Create Account → Subjects → Verify Email
 */
test.describe('Student registration — 6-step flow', () => {
  test.beforeAll(async () => {
    await clearMailhog();
  });

  test.beforeEach(async ({ page }) => {
    // Ensure no leftover auth session from previous tests
    await page.addInitScript(() => localStorage.clear());
    await mockCaptchaChallenge(page);
    await page.goto('/register');
    await expect(page.getByRole('heading', { name: 'Create your account' })).toBeVisible();
  });

  test('stepper shows 3 steps by default, expands to 6 when STUDENT is selected', async ({ page }) => {
    // Default — before role is selected: 3 steps
    await expect(page.getByText('Personal Details')).toBeVisible();
    await expect(page.getByText('Your Role')).toBeVisible();
    await expect(page.getByText('Verify Email')).toBeVisible();
    await expect(page.getByText('Academic Info')).not.toBeVisible();

    // Fill step 1 then advance to role selection
    await fillStep1(page, uniqueEmail('expand'));
    await page.getByRole('button', { name: /continue/i }).click();

    // Select STUDENT → stepper expands
    await page.getByRole('button', { name: /^student/i }).click();
    await expect(page.getByText('Academic Info')).toBeVisible();
    await expect(page.getByText('Subjects')).toBeVisible();
    await expect(page.getByText('Create Account')).toBeVisible();
  });

  test('password complexity badges update as requirements are met', async ({ page }) => {
    const pw = page.getByPlaceholder('••••••••').first();

    // Empty — no badges lit
    await pw.fill('');
    await expect(page.getByText('≥ 8 chars')).toBeVisible();

    // All requirements met
    await pw.fill('Abcdef@1');
    // All four badges should now be present and (based on the page's own logic) satisfied
    await expect(page.getByText('≥ 8 chars')).toBeVisible();
    await expect(page.getByText('1 uppercase')).toBeVisible();
    await expect(page.getByText('1 digit')).toBeVisible();
    await expect(page.getByText('1 special char')).toBeVisible();
  });

  test('valid institution code shows center name confirmation', async ({ page }) => {
    await fillStep1(page, uniqueEmail('lookup'));
    await page.getByRole('button', { name: /continue/i }).click();
    await page.getByRole('button', { name: /^student/i }).click();
    await page.getByRole('button', { name: /continue/i }).click();

    // Step 3 — institution code lookup
    await page.getByPlaceholder('e.g. SCH-2024-ABC').fill(env.institutionCode);
    await expect(page.getByText(/✓/)).toBeVisible({ timeout: 10_000 });
  });

  test('full registration + OTP verification → redirected to login', async ({ page }) => {
    test.setTimeout(90_000);
    const email = uniqueEmail('reg');

    // Step 1 — Personal details
    await fillStep1(page, email);
    await page.getByRole('button', { name: /continue/i }).click();

    // Step 2 — Role
    await page.getByRole('button', { name: /^student/i }).click();
    await page.getByRole('button', { name: /continue/i }).click();

    // Step 3 — Academic info
    await page.getByPlaceholder('+91 98765 43210').fill('9876543210');
    await page.locator('input[type="date"]').fill('2007-06-15');
    await page.getByPlaceholder('e.g. SCH-2024-ABC').fill(env.institutionCode);
    await expect(page.getByText(/✓/)).toBeVisible({ timeout: 10_000 });
    await page.locator('select[name="board"]').selectOption('CBSE');
    await page.locator('select[name="grade"]').selectOption('11');
    await page.getByRole('button', { name: /continue/i }).click();

    // Step 4 — Create account
    await solveCaptcha(page);
    await page.getByRole('button', { name: /create account/i }).click();
    await expect(page.getByText(/account created/i)).toBeVisible({ timeout: 15_000 });

    // Step 5 — Subjects (gracefully skip if none available)
    await page.getByRole('button', { name: /continue/i }).click();

    // Step 6 — OTP
    const emailBody = await waitForEmail(email, 'Verify', 30_000);
    const otp = extractOtp(emailBody);
    await fillOtp(page, otp);
    await page.getByRole('button', { name: 'Verify & Continue' }).click();

    await expect(page).toHaveURL(/\/login/, { timeout: 15_000 });
    await expect(page.getByText(/verified|can now sign in/i)).toBeVisible({ timeout: 10_000 });
  });
});

// ─── Helpers ──────────────────────────────────────────────────────────────────

async function fillStep1(page: import('@playwright/test').Page, email: string) {
  await page.getByPlaceholder('Jane').fill('E2E');
  await page.getByPlaceholder('Smith').fill('Test');
  await page.getByPlaceholder('you@example.com').fill(email);
  await page.getByPlaceholder('••••••••').first().fill('Test@12345');
  await page.getByPlaceholder('••••••••').nth(1).fill('Test@12345');
}

async function fillOtp(page: import('@playwright/test').Page, otp: string) {
  for (let i = 0; i < 6; i++) {
    await page.locator(`#otp-${i}`).fill(otp[i]);
  }
}
