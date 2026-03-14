import { test, expect } from '@playwright/test';
import { clearMailhog, extractOtp, waitForEmail } from './helpers/mailhog';
import { uniqueEmail } from './env';

/**
 * Forgot-password / password-reset E2E tests.
 *
 * These tests require:
 * - A running backend (auth-svc) with SMTP pointing to MailHog
 * - MailHog accessible at E2E_MAILHOG_API (default: http://localhost:8025)
 * - A pre-existing ACTIVE user for the change-password test
 *
 * The OTP is read from MailHog so no dev-console scraping is needed.
 */
test.describe('Forgot password flow', () => {
  test.beforeEach(async () => {
    await clearMailhog();
  });

  test('clicking "Forgot password?" on login page navigates to forgot-password page', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('link', { name: /forgot password/i }).click();
    await expect(page).toHaveURL('/forgot-password');
    await expect(page.getByRole('heading', { name: /forgot your password/i })).toBeVisible();
    await expect(page.getByPlaceholder('you@example.com')).toBeVisible();
  });

  test('invalid email shows validation error', async ({ page }) => {
    await page.goto('/forgot-password');
    await page.getByPlaceholder('you@example.com').fill('not-an-email');
    await page.getByRole('button', { name: /send reset code/i }).click();
    await expect(page.getByText(/valid email/i)).toBeVisible();
  });

  test('empty email shows required error', async ({ page }) => {
    await page.goto('/forgot-password');
    await page.getByRole('button', { name: /send reset code/i }).click();
    await expect(page.getByText(/email is required/i)).toBeVisible();
  });

  test('unknown email — submits without revealing existence — navigates to OTP page', async ({ page }) => {
    await page.goto('/forgot-password');
    await page.getByPlaceholder('you@example.com').fill('nobody@example.com');
    await page.getByRole('button', { name: /send reset code/i }).click();
    // Should navigate to /verify-otp regardless (no enumeration)
    await expect(page).toHaveURL('/verify-otp');
  });

  test('back to login link works', async ({ page }) => {
    await page.goto('/forgot-password');
    await page.getByRole('link', { name: /back to login/i }).click();
    await expect(page).toHaveURL('/login');
  });
});

/**
 * Full reset flow — requires MailHog SMTP and a real registered user.
 * Skipped in environments where mailhog is unavailable.
 */
test.describe('Full password reset flow (requires MailHog)', () => {
  const testEmail = uniqueEmail('pw-reset');

  test.skip(
    !process.env.E2E_MAILHOG_API,
    'Skipped — E2E_MAILHOG_API not set'
  );

  test.beforeEach(async () => {
    await clearMailhog();
  });

  test('receives OTP email after requesting password reset', async ({ page }) => {
    // This test assumes the user exists and is registered.
    // In CI the user is seeded; locally use qa-test@nexused.dev.
    await page.goto('/forgot-password');
    await page.getByPlaceholder('you@example.com').fill(testEmail);
    await page.getByRole('button', { name: /send reset code/i }).click();
    await expect(page).toHaveURL('/verify-otp');

    // Wait for OTP to arrive in MailHog
    const body = await waitForEmail(testEmail, 'Reset your EduTech password', 20_000);
    const otp = extractOtp(body);
    expect(otp).toMatch(/^\d{6}$/);
  });
});
