import { defineConfig, devices } from '@playwright/test';

/**
 * All configuration is driven by environment variables — no hardcoded values.
 *
 * Required for E2E runs (set in .env.e2e or CI secrets):
 *   E2E_BASE_URL              — frontend base URL (default: http://localhost:3000)
 *   CAPTCHA_E2E_BYPASS_TOKEN  — must match auth-svc CAPTCHA_E2E_BYPASS_TOKEN (non-empty)
 *   E2E_EXISTING_EMAIL        — existing verified student account email
 *   E2E_EXISTING_PASSWORD     — password for that account
 *   E2E_INSTITUTION_CODE      — valid institution code (e.g. NEXKOR001)
 *   E2E_MAILHOG_API           — MailHog API base URL (default: http://localhost:8025)
 */
export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false,        // registration tests create DB state — run sequentially
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['list'],
  ],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  outputDir: 'test-results',
});
