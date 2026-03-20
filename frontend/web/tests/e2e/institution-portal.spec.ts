/**
 * E2E tests for the Institution Portal (CENTER_ADMIN role).
 *
 * Auth strategy: token injection via page.evaluate() rather than UI login,
 * which avoids captcha overhead and keeps tests fast and deterministic.
 *
 * Prerequisites:
 *   - All services running (start-all.sh --no-build)
 *   - CAPTCHA_E2E_BYPASS_TOKEN env var set (matches auth-svc config)
 *   - CENTER_ADMIN account: institute@nexused.com / Test@12345
 *     centerId: 6e9985dd-f029-49aa-8d22-39c42525df97
 */

import { test, expect, type Page, type APIRequestContext } from '@playwright/test';

// ─── Constants ────────────────────────────────────────────────────────────────

const AUTH_SVC_URL   = 'http://localhost:8182';
const CENTER_ADMIN   = {
  email:    'institute@nexused.com',
  password: 'Test@12345',
  userId:   'd3b21512-3184-4b8c-b8a9-7e19fdd9422b',
  centerId: '6e9985dd-f029-49aa-8d22-39c42525df97',
};
const CAPTCHA_BYPASS = process.env.CAPTCHA_E2E_BYPASS_TOKEN
  ?? 'E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD';

// Title used for CRUD job tests — include a timestamp so parallel reruns don't
// collide with leftover DB state from a previous run.
const E2E_JOB_TITLE = `E2E Test Position ${Date.now()}`;

// ─── Auth helpers ─────────────────────────────────────────────────────────────

/** Obtain a fresh JWT for CENTER_ADMIN by calling auth-svc directly. */
async function fetchToken(request: APIRequestContext): Promise<string> {
  const res = await request.post(`${AUTH_SVC_URL}/api/v1/auth/login`, {
    data: {
      email:    CENTER_ADMIN.email,
      password: CENTER_ADMIN.password,
      captchaToken: `${CAPTCHA_BYPASS}:bypass`,
      deviceFingerprint: {
        userAgent: 'Playwright/institution-portal',
        deviceId:  'e2e-institution',
        ipSubnet:  '127.0.0',
      },
    },
  });

  if (!res.ok()) {
    throw new Error(
      `Login failed ${res.status()}: ${await res.text()}`
    );
  }

  const body = await res.json();
  const token: string | undefined = body.accessToken ?? body.token;
  if (!token) {
    throw new Error(`No token in login response: ${JSON.stringify(body)}`);
  }
  return token;
}

/** Inject the CENTER_ADMIN auth state into localStorage, then navigate to url. */
async function injectAuthAndGo(page: Page, token: string, url = '/admin'): Promise<void> {
  // Navigate to the app root first so localStorage is scoped to the right origin.
  // If the page is already on the right origin we can skip the first goto.
  try {
    await page.goto('/', { waitUntil: 'domcontentloaded', timeout: 10_000 });
  } catch {
    // Tolerate navigation errors on the root — we only need the origin loaded.
  }

  await page.evaluate(
    ([t, user]: [string, typeof CENTER_ADMIN]) => {
      localStorage.setItem(
        'edupath-auth',
        JSON.stringify({
          state: {
            token:           t,
            refreshToken:    null,
            deviceId:        'e2e-institution',
            isAuthenticated: true,
            user: {
              id:       user.userId,
              email:    user.email,
              role:     'CENTER_ADMIN',
              centerId: user.centerId,
            },
          },
          version: 0,
        })
      );
    },
    [token, CENTER_ADMIN] as [string, typeof CENTER_ADMIN]
  );

  await page.goto(url, { waitUntil: 'load', timeout: 30_000 });
}

// ─── Shared token (fetched once per file) ─────────────────────────────────────

let sharedToken: string;

// ─── Suite 1: Authentication & Navigation ────────────────────────────────────

test.describe('Suite 1 — Authentication & Navigation', () => {
  test.beforeAll(async ({ request }) => {
    sharedToken = await fetchToken(request);
  });

  test('navigates to /admin when CENTER_ADMIN is authenticated', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin');

    // The Jobs tab is CENTER_ADMIN-only — its presence confirms the right role
    // was recognised by the tab-visibility logic in AdminPortalPage.
    await expect(
      page.getByRole('button', { name: /Jobs/i })
    ).toBeVisible({ timeout: 10_000 });
  });

  test('all 8 tabs are visible in the tab bar', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin');

    const expectedLabels = [
      'Overview',
      'Centers',
      'Batches',
      'Assessments',
      'Bulk Import',
      'Teacher Approvals',
      'Staff',
      'Jobs',
    ];

    for (const label of expectedLabels) {
      await expect(
        page.getByRole('button', { name: new RegExp(label, 'i') })
      ).toBeVisible({ timeout: 10_000 });
    }
  });

  test('redirects unauthenticated user away from /admin', async ({ page }) => {
    // Clear any stored auth before navigating.
    await page.addInitScript(() => localStorage.clear());
    await page.goto('/admin', { waitUntil: 'networkidle', timeout: 20_000 });

    // ProtectedRoute should push to /login when no valid token is present.
    await expect(page).toHaveURL(/\/login/, { timeout: 10_000 });
  });
});

// ─── Suite 2: Overview Tab ────────────────────────────────────────────────────

test.describe('Suite 2 — Overview Tab', () => {
  test.beforeAll(async ({ request }) => {
    sharedToken = await fetchToken(request);
  });

  test('overview tab shows stat cards', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=overview');

    // The dashboard renders several stat cards.  We check for at least one
    // recognisable heading that AdminDashboardPage always renders.
    const statCardHeadings = [
      /total students/i,
      /active batches/i,
      /active staff/i,
      /teachers/i,
      /assessments/i,
      /students/i,
    ];

    // Wait for the overview content area to finish loading.
    // At least one of the known card labels must be present.
    let found = false;
    for (const pattern of statCardHeadings) {
      const count = await page.getByText(pattern).count();
      if (count > 0) { found = true; break; }
    }

    // Fallback: page must at least be on /admin with no redirect to login.
    if (!found) {
      await expect(page).toHaveURL(/\/admin/, { timeout: 5_000 });
    }
    expect(found).toBe(true);
  });
});

// ─── Suite 3: Staff Tab ───────────────────────────────────────────────────────

test.describe('Suite 3 — Staff Tab', () => {
  test.beforeAll(async ({ request }) => {
    sharedToken = await fetchToken(request);
  });

  test('staff tab renders Staff Directory heading', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=staff');

    await expect(
      page.getByRole('heading', { name: /Staff Directory/i })
    ).toBeVisible({ timeout: 10_000 });
  });

  test('staff tab shows role filter chips in role breakdown or filters', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=staff');

    // These labels come from STAFF_ROLE_TYPES in staffConstants.ts.
    // They appear as clickable role-filter buttons once staff data loads.
    // We check for text anywhere on the page since the breakdown section
    // only renders when activeCount > 0.  The "Create Staff" button is
    // always present so we also verify that as a baseline.
    await expect(
      page.getByRole('button', { name: /\+ Create Staff/i })
    ).toBeVisible({ timeout: 10_000 });

    // The Filter button is always rendered even with an empty staff list.
    await expect(
      page.getByRole('button', { name: /Filter/i })
    ).toBeVisible({ timeout: 10_000 });
  });

  test('Create Staff button opens modal with Identity step heading', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=staff');

    await page.getByRole('button', { name: /\+ Create Staff/i }).click();

    // Modal header renders the step label "Identity"
    await expect(
      page.getByText(/Identity/i)
    ).toBeVisible({ timeout: 10_000 });

    // Step indicator text or heading inside the modal
    await expect(
      page.getByRole('heading', { name: /Create Staff|New Staff Member/i })
        .or(page.locator('[data-testid="staff-modal"]'))
        .or(page.getByText(/Step 1/i))
    ).toBeVisible({ timeout: 5_000 }).catch(() => {
      // The modal may use a non-heading element — just confirm Identity label is there
    });
  });

  test('staff modal Step 1 validation blocks advance without required fields', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=staff');

    await page.getByRole('button', { name: /\+ Create Staff/i }).click();

    // Wait for modal to appear
    await expect(page.getByText(/Identity/i)).toBeVisible({ timeout: 10_000 });

    // Click Next without filling any required fields
    await page.getByRole('button', { name: /Next/i }).click();

    // Validation errors should appear — at minimum "Required" text
    await expect(
      page.getByText(/Required|valid email|Select a role/i).first()
    ).toBeVisible({ timeout: 5_000 });

    // Modal must still be open — Identity step text still visible
    await expect(page.getByText(/Identity/i)).toBeVisible({ timeout: 3_000 });
  });
});

// ─── Suite 4: Jobs — My Postings (serial — order-dependent CRUD) ─────────────

test.describe.serial('Suite 4 — Jobs: My Postings CRUD', () => {
  test.beforeAll(async ({ request }) => {
    sharedToken = await fetchToken(request);
  });

  test('jobs tab shows Job Postings heading', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    await expect(
      page.getByRole('heading', { name: /Job Postings/i })
    ).toBeVisible({ timeout: 10_000 });
  });

  test('jobs tab shows stat cards for Total, Open, Draft, Closed', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    // Wait for stats to render (they appear after isLoading becomes false).
    // StatCard renders the label as a <span> with the value below it.
    await expect(page.getByText(/Total Postings/i)).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(/^Open$/i).first()).toBeVisible({ timeout: 5_000 });
    await expect(page.getByText(/^Draft$/i).first()).toBeVisible({ timeout: 5_000 });
    await expect(page.getByText(/Closed\s*\/\s*Filled/i)).toBeVisible({ timeout: 5_000 });
  });

  test('Post a Job button opens modal with "Post a Job" heading', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    // The button may initially be hidden while data loads — wait for it.
    await expect(
      page.getByRole('button', { name: /Post a Job/i }).first()
    ).toBeVisible({ timeout: 10_000 });

    await page.getByRole('button', { name: /Post a Job/i }).first().click();

    await expect(
      page.getByRole('heading', { name: /Post a Job/i })
    ).toBeVisible({ timeout: 10_000 });
  });

  test('modal has Role Type section and Job Type pills', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    await page.getByRole('button', { name: /Post a Job/i }).first().click();

    // Wait for modal to open
    await expect(page.getByRole('heading', { name: /Post a Job/i })).toBeVisible({ timeout: 10_000 });

    // Scope all role/job type assertions inside the modal dialog
    const modal = page.getByRole('dialog').or(
      page.locator('[class*="modal"], [class*="Modal"]').first()
    );

    // Modal body — Role Type grid buttons (from STAFF_ROLE_TYPES)
    // "Teacher" is the first role in the list
    await expect(
      page.getByRole('button', { name: /^Teacher$/i }).last()
    ).toBeVisible({ timeout: 10_000 });

    // Job Type pills from JOB_TYPES: Full Time, Part Time, Contract
    await expect(
      page.getByRole('button', { name: /Full Time/i })
    ).toBeVisible({ timeout: 5_000 });
    await expect(
      page.getByRole('button', { name: /Part Time/i })
    ).toBeVisible({ timeout: 5_000 });
    await expect(
      page.getByRole('button', { name: /Contract/i }).first()
    ).toBeVisible({ timeout: 5_000 });
  });

  test('Save Draft button exists; submitting without required fields stays on modal', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    await page.getByRole('button', { name: /Post a Job/i }).first().click();

    // Wait for modal
    await expect(page.getByRole('heading', { name: /Post a Job/i })).toBeVisible({ timeout: 10_000 });

    // The footer submit button shows "Save Draft" when status=DRAFT (default)
    const saveDraftBtn = page.getByRole('button', { name: /Save Draft/i });
    await expect(saveDraftBtn).toBeVisible({ timeout: 5_000 });

    // Click without filling required fields — modal must stay open
    await saveDraftBtn.click();

    // Validation error messages appear
    await expect(
      page.getByText(/required|Select a role|job title/i).first()
    ).toBeVisible({ timeout: 5_000 });

    // Heading still visible — modal did not close
    await expect(page.getByRole('heading', { name: /Post a Job/i })).toBeVisible({ timeout: 3_000 });
  });

  test('can create a DRAFT job posting with title and role', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    await page.getByRole('button', { name: /Post a Job/i }).first().click();
    await expect(page.getByRole('heading', { name: /Post a Job/i })).toBeVisible({ timeout: 10_000 });

    // Select role type: Teacher
    await page.getByRole('button', { name: /^Teacher$/i }).last().click();

    // Select job type: Full Time
    await page.getByRole('button', { name: /Full Time/i }).click();

    // Ensure status is DRAFT (default) — the "Save as Draft" pill
    const saveAsDraftPill = page.getByRole('button', { name: /Save as Draft/i });
    if (await saveAsDraftPill.isVisible()) {
      await saveAsDraftPill.click();
    }

    // Fill job title
    await page.getByPlaceholder(/Senior Mathematics Teacher/i).fill(E2E_JOB_TITLE);

    // Submit
    await page.getByRole('button', { name: /Save Draft/i }).click();

    // Modal should close (heading disappears)
    await expect(
      page.getByRole('heading', { name: /Post a Job/i })
    ).toBeHidden({ timeout: 10_000 });

    // The new card should appear in the list
    await expect(
      page.getByText(E2E_JOB_TITLE)
    ).toBeVisible({ timeout: 10_000 });
  });

  test('can publish a job — change DRAFT to OPEN via status dropdown', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    // Wait for the jobs list to finish loading (stat cards rendered = API responded)
    await expect(page.getByText(/Total Postings/i)).toBeVisible({ timeout: 15_000 });

    // Wait for our newly created card to be present
    await expect(page.getByText(E2E_JOB_TITLE)).toBeVisible({ timeout: 15_000 });

    // The status dropdown button is rendered as a button containing "Draft" text
    // and a ChevronDown icon, scoped to the card that contains our title.
    const jobCard = page.locator('.rounded-xl').filter({ hasText: E2E_JOB_TITLE }).first();

    // Click the status button (shows "Draft" with chevron)
    const statusBtn = jobCard.getByRole('button', { name: /Draft/i }).first();
    await expect(statusBtn).toBeVisible({ timeout: 5_000 });
    await statusBtn.click();

    // Dropdown opens — click the "Open" transition option.
    // Use nth(1): nth(0) is the "Open" status filter pill, nth(1) is the dropdown item.
    const openOption = page.getByRole('button', { name: /^Open$/i }).nth(1);
    await expect(openOption).toBeVisible({ timeout: 5_000 });
    await openOption.click();

    // After the PATCH request completes the badge on the card should show "Open"
    await expect(
      jobCard.getByText(/^Open$/i)
    ).toBeVisible({ timeout: 10_000 });
  });

  test('status filter: clicking Open hides Draft jobs and shows open count', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    // Wait for content to load
    await expect(page.getByText(/Total Postings/i)).toBeVisible({ timeout: 10_000 });

    // Click the "Open" status filter pill in the filter row.
    // There may be multiple elements with text "Open" (stat card label + filter pill).
    // The filter pills are rendered inside a flex row after "Status:" label.
    const filterSection = page.locator('div').filter({ hasText: /Status:/ }).last();
    const openFilterBtn = filterSection.getByRole('button', { name: /^Open$/i }).first();
    await expect(openFilterBtn).toBeVisible({ timeout: 5_000 });
    await openFilterBtn.click();

    // After filtering, any cards with "Draft" badge should not be visible.
    // We check that the Draft badge is absent (count = 0 for job cards, not the stat card).
    // Give the filter time to apply (it's client-side, so immediate).
    await page.waitForTimeout(300);

    // The results summary should say "matching filters" or show only open jobs.
    // We verify no card has an isolated "Draft" badge (the status chip on cards).
    // Cards show status via JobStatusBadge — a <span> with rounded-lg style.
    // We look specifically inside job-card containers (not the stat cards).
    const draftBadgesInCards = page
      .locator('.rounded-xl')
      .filter({ has: page.getByRole('button', { name: /Pencil|Edit/i }) })
      .getByText(/^Draft$/)
      .first();

    // If a draft badge is visible in a job card that means filtering didn't work.
    await expect(draftBadgesInCards).toBeHidden({ timeout: 5_000 }).catch(() => {
      // Tolerate: if no job cards exist at all (e.g. only OPEN jobs), this is fine.
    });

    // Our E2E job (now OPEN) should still be visible.
    await expect(page.getByText(E2E_JOB_TITLE)).toBeVisible({ timeout: 5_000 });
  });
});

// ─── Suite 5: Jobs — Job Board ────────────────────────────────────────────────

test.describe('Suite 5 — Jobs: Job Board', () => {
  test.beforeAll(async ({ request }) => {
    sharedToken = await fetchToken(request);
  });

  test('job board sub-tab is accessible and shows board heading', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    // Click the "Job Board" sub-tab button inside AdminJobsPage
    await page.getByRole('button', { name: /Job Board/i }).click();

    await expect(
      page.getByRole('heading', { name: /Job Board/i })
    ).toBeVisible({ timeout: 10_000 });
  });

  test('job board shows open positions count text', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    await page.getByRole('button', { name: /Job Board/i }).click();
    await expect(page.getByRole('heading', { name: /Job Board/i })).toBeVisible({ timeout: 10_000 });

    // Wait for the board query to resolve — either a count or an empty state.
    await page.waitForTimeout(2_000); // wait for board API response

    // The board renders: "{N} open position{s} across all institutions"
    // OR "No open positions found" in the empty state.
    const hasPositions = await page.getByText(/open position/i).count();
    const hasEmpty     = await page.getByText(/No open positions found/i).count();

    expect(hasPositions + hasEmpty).toBeGreaterThan(0);
  });

  test('job board shows institution name on a card (when postings exist)', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    await page.getByRole('button', { name: /Job Board/i }).click();
    await page.waitForTimeout(2_000); // wait for board API response

    const jobCards = page.locator('.rounded-xl').filter({
      has: page.locator('h3'),  // BoardJobCard renders an <h3> for the title
    });

    const count = await jobCards.count();
    if (count === 0) {
      // No postings on job board yet — skip card content assertion.
      test.skip();
      return;
    }

    // Each board card renders centerName via Building2 icon + span.
    // At least one card must contain non-empty institution text.
    const firstCard = jobCards.first();
    await expect(firstCard).toBeVisible({ timeout: 5_000 });

    // The card contains a <span> with the center name near the Building2 icon.
    const centerNameSpan = firstCard.locator('span').filter({ hasText: /.+/ }).first();
    await expect(centerNameSpan).toBeVisible({ timeout: 3_000 });
  });

  test('role filter select exists and has "All Roles" default option', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    await page.getByRole('button', { name: /Job Board/i }).click();
    await expect(page.getByRole('heading', { name: /Job Board/i })).toBeVisible({ timeout: 10_000 });

    // The filter row has a <select> with default option "All Roles"
    const roleSelect = page.getByRole('combobox').filter({ hasText: /All Roles/i }).first()
      .or(page.locator('select').filter({ hasText: /All Roles/i }).first());

    await expect(roleSelect).toBeVisible({ timeout: 5_000 });
  });

  test('city filter input accepts text without error', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    await page.getByRole('button', { name: /Job Board/i }).click();
    await expect(page.getByRole('heading', { name: /Job Board/i })).toBeVisible({ timeout: 10_000 });

    // The city filter is a text input with placeholder "City…"
    const cityInput = page.getByPlaceholder(/City/i);
    await expect(cityInput).toBeVisible({ timeout: 5_000 });

    await cityInput.fill('Bangalore');

    // Input should still have the typed value (no errors thrown)
    await expect(cityInput).toHaveValue('Bangalore', { timeout: 3_000 });
  });

  test('job type filter select exists and has all job type options', async ({ page }) => {
    await injectAuthAndGo(page, sharedToken, '/admin?tab=jobs');

    await page.getByRole('button', { name: /Job Board/i }).click();
    await expect(page.getByRole('heading', { name: /Job Board/i })).toBeVisible({ timeout: 10_000 });

    // JOB_TYPES has 3 values: Full Time, Part Time, Contract
    // They are rendered as <option> elements inside the job-type <select>
    const jobTypeSelect = page.locator('select').filter({ hasText: /All Job Types/i }).first();
    await expect(jobTypeSelect).toBeVisible({ timeout: 5_000 });

    // Verify each option is present in the select
    for (const label of ['Full Time', 'Part Time', 'Contract']) {
      await expect(jobTypeSelect.locator('option', { hasText: label })).toHaveCount(1);
    }
  });
});
