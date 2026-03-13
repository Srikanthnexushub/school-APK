import type { Page } from '@playwright/test';
import { env } from '../env';

/**
 * Intercept GET /api/v1/captcha/challenge and return the E2E bypass token as
 * the challenge ID.  The frontend widget then calls onVerify with
 * `{bypassToken}:{answer}`, which the backend accepts without a Redis lookup
 * when CAPTCHA_E2E_BYPASS_TOKEN matches.
 *
 * Call this before navigating to any page that renders CaptchaWidget.
 */
export async function mockCaptchaChallenge(page: Page): Promise<void> {
  await page.route('**/api/v1/captcha/challenge', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: env.captchaBypass,
        // Minimal 1×1 white PNG so the <img> renders without a broken-image icon
        imageDataUri:
          'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwADhQGAWjR9awAAAABJRU5ErkJggg==',
      }),
    })
  );
}

/**
 * Fill the captcha answer input with exactly 6 uppercase characters.
 * The widget calls onVerify once 6 chars are entered, producing the token
 * `{bypassToken}:BYPASS` which the backend accepts.
 */
export async function solveCaptcha(page: Page): Promise<void> {
  const input = page.getByPlaceholder('Enter 6 characters');
  await input.fill('BYPASS');
}
