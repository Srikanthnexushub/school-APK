import { env } from '../env';

interface MailhogMessage {
  ID: string;
  Content: {
    Headers: { Subject: string[]; To: string[] };
    Body: string;
  };
  Created: string;
}

interface MailhogApiResponse {
  items: MailhogMessage[];
  total: number;
}

/**
 * Poll MailHog until an email addressed to `toAddress` arrives that contains
 * `subjectContains` in its subject line.  Returns the full message body.
 *
 * Throws after `timeoutMs` milliseconds if no matching email is found.
 */
export async function waitForEmail(
  toAddress: string,
  subjectContains: string,
  timeoutMs = 30_000,
  pollIntervalMs = 1_000
): Promise<string> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const res = await fetch(`${env.mailhogApi}/api/v2/messages?limit=50`);
    if (res.ok) {
      const data: MailhogApiResponse = await res.json();
      const match = data.items.find((msg) => {
        const to = (msg.Content.Headers.To ?? []).join(',').toLowerCase();
        const subject = (msg.Content.Headers.Subject ?? []).join(' ');
        return to.includes(toAddress.toLowerCase()) && subject.includes(subjectContains);
      });
      if (match) return match.Content.Body;
    }
    await new Promise((r) => setTimeout(r, pollIntervalMs));
  }
  throw new Error(`No email to ${toAddress} with subject containing "${subjectContains}" within ${timeoutMs}ms`);
}

/** Extract the first 6-digit OTP code from an email body. */
export function extractOtp(body: string): string {
  const match = body.match(/\b(\d{6})\b/);
  if (!match) throw new Error(`No 6-digit OTP found in email body:\n${body}`);
  return match[1];
}

/** Delete all MailHog messages (useful as a beforeAll cleanup). */
export async function clearMailhog(): Promise<void> {
  await fetch(`${env.mailhogApi}/api/v1/messages`, { method: 'DELETE' });
}
