/**
 * Typed access to all E2E environment variables.
 * Throws at test-start time if a required variable is missing,
 * so failures are obvious rather than manifesting as strange test errors.
 */

function required(name: string): string {
  const val = process.env[name];
  if (!val) throw new Error(`E2E env var ${name} is required but not set`);
  return val;
}

function optional(name: string, defaultValue: string): string {
  return process.env[name] || defaultValue;
}

export const env = {
  baseUrl:           optional('E2E_BASE_URL', 'http://localhost:3000'),
  captchaBypass:     required('CAPTCHA_E2E_BYPASS_TOKEN'),
  existingEmail:     optional('E2E_EXISTING_EMAIL', 'qa-test@nexused.dev'),
  existingPassword:  optional('E2E_EXISTING_PASSWORD', 'Test@12345'),
  institutionCode:   optional('E2E_INSTITUTION_CODE', 'NEXKOR001'),
  mailhogApi:        optional('E2E_MAILHOG_API', 'http://localhost:8025'),
} as const;

/** Generate a unique email address for each test run to avoid duplicate-account errors. */
export function uniqueEmail(prefix = 'e2e'): string {
  return `${prefix}.${Date.now()}@nexused.dev`;
}
