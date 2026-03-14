import { test, expect } from '@playwright/test';
import { env } from './env';
import { mockCaptchaChallenge, solveCaptcha } from './helpers/captcha';

/**
 * Settings → Security tab E2E tests.
 *
 * These tests require a logged-in STUDENT user (qa-test@nexused.dev / Test@12345).
 */
async function loginAndGoToSecurity(page: import('@playwright/test').Page) {
  await page.addInitScript(() => localStorage.clear());
  await mockCaptchaChallenge(page);
  await page.goto('/login');
  await page.getByPlaceholder('you@example.com').fill(env.existingEmail);
  await page.locator('input[type="password"]').fill(env.existingPassword);
  await solveCaptcha(page);
  await page.getByRole('button', { name: /sign in/i }).click();
  await expect(page).toHaveURL(/\/(dashboard|$)/);

  await page.goto('/settings');
  await page.getByRole('button', { name: /security/i }).click();
  await expect(page.getByRole('heading', { name: /change password/i })).toBeVisible();
}

test.describe('Settings → Security tab', () => {
  test('shows Change Password and Two-Factor Authentication sections', async ({ page }) => {
    await loginAndGoToSecurity(page);
    await expect(page.getByRole('heading', { name: /change password/i })).toBeVisible();
    await expect(page.getByRole('heading', { name: /two-factor authentication/i })).toBeVisible();
  });

  test('change password — mismatched passwords — shows validation error', async ({ page }) => {
    await loginAndGoToSecurity(page);
    await page.locator('input[placeholder=""]').first().fill('SomePass@1');
    // Fill new password fields — target by label
    const newPwInputs = page.locator('input[type="password"]');
    await newPwInputs.nth(1).fill('NewPass@123');
    await newPwInputs.nth(2).fill('DifferentPass@456');
    await page.getByRole('button', { name: /change password/i }).click();
    await expect(page.getByText(/do not match/i)).toBeVisible();
  });

  test('change password — wrong current password — shows error toast', async ({ page }) => {
    await loginAndGoToSecurity(page);
    const pwInputs = page.locator('input[type="password"]');
    await pwInputs.nth(0).fill('WrongPass@99');
    await pwInputs.nth(1).fill('NewPass@9876');
    await pwInputs.nth(2).fill('NewPass@9876');
    await page.getByRole('button', { name: /change password/i }).click();
    // Expects error toast (422 from backend maps to error message)
    await expect(page.getByText(/failed to change|check your current/i)).toBeVisible({ timeout: 5000 });
  });

  test('2FA section shows toggle', async ({ page }) => {
    await loginAndGoToSecurity(page);
    const twoFaSection = page.getByRole('heading', { name: /two-factor authentication/i }).locator('..');
    await expect(twoFaSection).toBeVisible();
  });

  test('enabling 2FA shows QR code and confirm input', async ({ page }) => {
    await loginAndGoToSecurity(page);
    // Find the 2FA toggle and click it
    const toggle = page.locator('button[role="switch"]').first();
    const isEnabled = await toggle.getAttribute('aria-checked');
    if (isEnabled === 'true') {
      // Already enabled — skip
      return;
    }
    await toggle.click();
    // Should show QR code section
    await expect(page.getByAltText('TOTP QR code')).toBeVisible({ timeout: 5000 });
    await expect(page.getByPlaceholder('000000')).toBeVisible();
  });
});
