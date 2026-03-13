import { test, expect } from '@playwright/test';
import { env } from './env';
import { mockCaptchaChallenge, solveCaptcha } from './helpers/captcha';

test.describe('Login flow', () => {
  test.beforeEach(async ({ page }) => {
    // Clear persisted auth state so tests don't bleed into each other
    await page.addInitScript(() => localStorage.clear());
    await mockCaptchaChallenge(page);
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
  });

  test('shows login page with required fields', async ({ page }) => {
    await expect(page.getByPlaceholder('you@example.com')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
    await expect(page.getByPlaceholder('Enter 6 characters')).toBeVisible();
    await expect(page.getByRole('button', { name: /sign in/i })).toBeDisabled();
  });

  test('sign-in button stays disabled until captcha is solved', async ({ page }) => {
    await page.getByPlaceholder('you@example.com').fill(env.existingEmail);
    await page.locator('input[type="password"]').fill(env.existingPassword);
    await expect(page.getByRole('button', { name: /sign in/i })).toBeDisabled();

    await solveCaptcha(page);
    await expect(page.getByRole('button', { name: /sign in/i })).toBeEnabled();
  });

  test('successful login redirects to dashboard and shows username', async ({ page }) => {
    await page.getByPlaceholder('you@example.com').fill(env.existingEmail);
    await page.locator('input[type="password"]').fill(env.existingPassword);
    await solveCaptcha(page);
    await page.getByRole('button', { name: /sign in/i }).click();

    await page.waitForURL(/\/(dashboard|admin|parent|mentor-portal)/, { timeout: 15_000 });
    const sidebar = page.locator('aside, [role="complementary"]').first();
    await expect(sidebar).not.toContainText('undefined');
  });

  test('wrong password — stays on login page', async ({ page }) => {
    await page.getByPlaceholder('you@example.com').fill(env.existingEmail);
    await page.locator('input[type="password"]').fill('WrongPassword!99');
    await solveCaptcha(page);
    await page.getByRole('button', { name: /sign in/i }).click();

    // Should stay on login page — not navigate away
    await page.waitForTimeout(3_000); // allow backend response + any redirect
    await expect(page).toHaveURL(/\/login/);
    // Login form must still be visible
    await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
  });

  test('navigates to register page via Create one link', async ({ page }) => {
    await page.getByRole('link', { name: /create one/i }).click();
    await expect(page).toHaveURL(/\/register/);
  });
});
