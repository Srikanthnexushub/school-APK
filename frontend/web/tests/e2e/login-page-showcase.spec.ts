/**
 * Login Page Showcase — E2E tests (login-page-showcase.spec.ts)
 *
 * Validates the public /login page across four areas:
 *   Suite 1 — Text & Structure        (5 tests)
 *   Suite 2 — Feature Carousel        (6 tests)  desktop 1280×800
 *   Suite 3 — Social buttons always visible (3 tests)
 *   Suite 4 — Form regression guards  (3 tests)  FROZEN Fix #40 constraints
 *
 * No auth injection needed — /login is a public route.
 *
 * Prerequisites:
 *   - Frontend running on localhost:3000  (start-all.sh --no-build)
 */

import { test, expect } from '@playwright/test';

// ─── Constants ────────────────────────────────────────────────────────────────

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL ?? process.env.E2E_BASE_URL ?? 'http://localhost:3000';
const LOGIN_URL = `${BASE_URL}/login`;

// ─── Suite 1 — Text & Structure ───────────────────────────────────────────────

test.describe('Suite 1 — Text & Structure', () => {
  test('renders "study companion" — NOT "exam companion"', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    // The left-panel hero heading should advertise the product as a "study companion".
    // "exam companion" would be the wrong copy — this test guards against regressions.
    await expect(page.locator('text=/study companion/i').first()).toBeVisible();
    await expect(page.locator('text=/exam companion/i').first()).not.toBeVisible();
  });

  test('shows NexusEd branding', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    await expect(page.locator('text=NexusEd').first()).toBeVisible();
  });

  test('shows "Welcome back" heading', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    await expect(page.getByRole('heading', { name: /welcome back/i })).toBeVisible();
  });

  test('forgot password link navigates to /forgot-password', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    const forgotLink = page.getByRole('link', { name: /forgot password/i });
    await expect(forgotLink).toBeVisible();
    await forgotLink.click();
    await page.waitForURL('**/forgot-password', { timeout: 10_000 });
    expect(page.url()).toContain('/forgot-password');
  });

  test('register link navigates to /register', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    // "Create one" is the register CTA text in the current implementation
    const registerLink = page.getByRole('link', { name: /create one|sign up|register/i });
    await expect(registerLink).toBeVisible();
    await registerLink.click();
    await page.waitForURL('**/register', { timeout: 10_000 });
    expect(page.url()).toContain('/register');
  });
});

// ─── Suite 2 — Feature Carousel ───────────────────────────────────────────────
// Uses a desktop viewport of 1280×800 so the left panel (lg:flex) is rendered.

test.describe('Suite 2 — Feature Carousel', () => {
  test.use({ viewport: { width: 1280, height: 800 } });

  test('First feature "Psychometric Intelligence" is visible on load', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    await expect(page.locator('text=/psychometric intelligence/i').first()).toBeVisible();
  });

  test('Carousel auto-advances after 4s — "AI Study Mentor" appears', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    // Wait longer than the carousel rotation interval (4 s) before asserting
    await page.waitForTimeout(4500);
    await expect(page.locator('text=/ai study mentor/i').first()).toBeVisible();
  });

  test('Dot navigation renders 8 buttons with aria-label "Feature N"', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    // Each dot should have aria-label "Feature 1" through "Feature 8"
    for (let i = 1; i <= 8; i++) {
      await expect(
        page.getByRole('button', { name: `Feature ${i}` })
      ).toBeVisible();
    }
  });

  test('Clicking "Feature 3" dot shows "Career Oracle"', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    await page.getByRole('button', { name: 'Feature 3' }).click();
    await expect(page.locator('text=/career oracle/i').first()).toBeVisible();
  });

  test('Next arrow (aria-label="Next feature") advances to "AI Study Mentor"', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    // Click the next arrow once from the first slide ("Psychometric Intelligence")
    await page.getByRole('button', { name: 'Next feature' }).click();
    await expect(page.locator('text=/ai study mentor/i').first()).toBeVisible();
  });

  test('Prev arrow (aria-label="Previous feature") from first wraps to "Real-time Intelligence"', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    // On the first slide, pressing Prev should wrap around to the last slide
    await page.getByRole('button', { name: 'Previous feature' }).click();
    await expect(page.locator('text=/real-time intelligence/i').first()).toBeVisible();
  });
});

// ─── Suite 3 — Social buttons always visible ──────────────────────────────────
// Social sign-in buttons (Google + GitHub) are ALWAYS rendered regardless of
// whether VITE_GOOGLE_CLIENT_ID / VITE_GITHUB_CLIENT_ID are set.
// When unconfigured they show an informational toast instead of redirecting.

test.describe('Suite 3 — Social buttons always visible', () => {
  test('GitHub button is always visible (even without VITE_GITHUB_CLIENT_ID)', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    // GitHub button is always rendered — it shows a toast when env var is unset
    await expect(
      page.getByRole('button', { name: /github/i })
    ).toBeVisible();
  });

  test('Google button is always visible (even without VITE_GOOGLE_CLIENT_ID)', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    // Google button is always rendered — either the real GoogleSignInButton or a fallback
    await expect(
      page.getByRole('button', { name: /google/i })
    ).toBeVisible();
  });

  test('Social divider "or sign in with email" is always visible', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    await expect(
      page.locator('text=/or sign in with email/i').first()
    ).toBeVisible();
  });
});

// ─── Suite 4 — Form regression guards ────────────────────────────────────────
// FROZEN constraints from Fix #40 (commit b54547a).
// ⛔ NEVER modify these tests without explicit permission — they guard against
//    autofill regressions that leak credentials into browser password managers.

test.describe('Suite 4 — Form regression guards', () => {
  test('password input has autocomplete="new-password" (Fix #40 regression guard)', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    // Fix #40: autoComplete="new-password" prevents browser from pre-filling password.
    // Using "current-password" would invite autofill — NEVER change this expectation.
    const passwordInput = page.locator('input[type="password"], input[type="text"][placeholder="••••••••"]').first();
    await expect(passwordInput).toHaveAttribute('autocomplete', 'new-password');
  });

  test('email input has autocomplete="email" (Fix #40 regression guard)', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    const emailInput = page.locator('input[type="email"]').first();
    await expect(emailInput).toHaveAttribute('autocomplete', 'email');
  });

  test('Sign in button is disabled when captcha not yet completed', async ({ page }) => {
    await page.goto(LOGIN_URL, { waitUntil: 'load' });
    // The submit button is disabled={isSubmitting || !captchaToken}.
    // On fresh page load captchaToken is null, so the button must start disabled.
    const signInBtn = page.getByRole('button', { name: /sign in/i }).last();
    await expect(signInBtn).toBeDisabled();
  });
});
