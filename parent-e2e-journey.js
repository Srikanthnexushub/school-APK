const { chromium } = require('/Users/srikanth/IdeaProjects/school-APK/frontend/web/node_modules/playwright');
const https = require('https');
const http = require('http');

const BASE_URL = 'http://localhost:3000';
const SCREENSHOTS_DIR = '/Users/srikanth/IdeaProjects/school-APK';

const CAPTCHA_ID = 'c24ebcf5-70ea-4eac-ab4f-d93e179b0c7f';
const CAPTCHA_ANSWER = '2EK3FW';

async function httpPost(url, body) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const urlObj = new URL(url);
    const options = {
      hostname: urlObj.hostname,
      port: urlObj.port,
      path: urlObj.pathname,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(data),
      },
    };
    const req = http.request(options, (res) => {
      let responseData = '';
      res.on('data', (chunk) => (responseData += chunk));
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(responseData) });
        } catch (e) {
          resolve({ status: res.statusCode, body: responseData });
        }
      });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

async function waitForLoadingToDisappear(page) {
  // Wait for common loading spinners to disappear
  try {
    await page.waitForSelector('.loading, [data-testid="loading"], .spinner, [aria-busy="true"]', {
      state: 'detached',
      timeout: 5000
    });
  } catch (e) {
    // No spinner found, that's fine
  }
  // Small delay to let content render
  await page.waitForTimeout(1500);
}

async function main() {
  console.log('Starting Parent Portal E2E Journey...');

  // Step 1: Login via API
  console.log('Step 1: Logging in via API...');
  const loginPayload = {
    email: 'parent1@test.com',
    password: 'Test@12345',
    captchaToken: `${CAPTCHA_ID}:${CAPTCHA_ANSWER}`,
    deviceFingerprint: {
      userAgent: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      deviceId: 'playwright-e2e-parent-device-001',
      ipSubnet: '127.0.0.1/24',
    },
  };

  const loginResponse = await httpPost(`${BASE_URL}/api/v1/auth/login`, loginPayload);
  console.log('Login response status:', loginResponse.status);
  console.log('Login response body keys:', Object.keys(loginResponse.body || {}));

  if (loginResponse.status !== 200 || (!loginResponse.body.token && !loginResponse.body.accessToken)) {
    console.error('Login failed:', JSON.stringify(loginResponse.body, null, 2));
    process.exit(1);
  }

  const token = loginResponse.body.token || loginResponse.body.accessToken;
  const user = loginResponse.body.user || {};
  console.log('Login successful! Token obtained.');

  // Launch browser
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1400, height: 900 } });
  const page = await context.newPage();

  // Navigate to app first to set localStorage
  await page.goto(BASE_URL);
  await page.waitForLoadState('domcontentloaded');

  // Screenshot 1: Login page (before injecting token)
  console.log('Screenshot 1: Login page...');
  await page.goto(`${BASE_URL}/login`);
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(2000);
  await page.screenshot({ path: `${SCREENSHOTS_DIR}/parent-journey-01-login.png`, fullPage: false });
  console.log('  Saved parent-journey-01-login.png');

  // Inject token into localStorage
  console.log('Injecting JWT into localStorage...');
  await page.evaluate(({ token, user }) => {
    localStorage.setItem('edupath-auth', JSON.stringify({
      state: {
        token,
        user: {
          id: user.id || '30a06234-4e3d-4fa0-91a3-7705646c2b21',
          email: 'parent1@test.com',
          role: 'PARENT',
          name: user.name || 'Parent User',
          ...user,
        },
        isAuthenticated: true,
      },
      version: 0,
    }));
  }, { token, user });

  // Screenshot 2: Parent Dashboard
  console.log('Screenshot 2: Parent Dashboard...');
  await page.goto(`${BASE_URL}/parent`);
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(3000);
  await page.screenshot({ path: `${SCREENSHOTS_DIR}/parent-journey-02-dashboard.png`, fullPage: false });
  console.log('  Saved parent-journey-02-dashboard.png');

  // Screenshot 3: My Children
  console.log('Screenshot 3: My Children...');
  await page.goto(`${BASE_URL}/parent/children`);
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(3000);
  await page.screenshot({ path: `${SCREENSHOTS_DIR}/parent-journey-03-children.png`, fullPage: false });
  console.log('  Saved parent-journey-03-children.png');

  // Screenshot 4: Link Child Modal
  console.log('Screenshot 4: Link Child modal...');
  // Try to find and click "Link Child" button
  try {
    const linkChildBtn = await page.locator('button:has-text("Link Child"), button:has-text("Add Child"), button:has-text("Link"), [data-testid="link-child"]').first();
    if (await linkChildBtn.isVisible()) {
      await linkChildBtn.click();
      await page.waitForTimeout(1000);
    }
  } catch (e) {
    console.log('  Link Child button not found, taking screenshot of current state');
  }
  await page.screenshot({ path: `${SCREENSHOTS_DIR}/parent-journey-04-link-modal.png`, fullPage: false });
  console.log('  Saved parent-journey-04-link-modal.png');

  // Close modal if open (press Escape)
  await page.keyboard.press('Escape');
  await page.waitForTimeout(500);

  // Screenshot 5: Fees page
  console.log('Screenshot 5: Fees page...');
  await page.goto(`${BASE_URL}/parent/fees`);
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(3000);
  await page.screenshot({ path: `${SCREENSHOTS_DIR}/parent-journey-05-fees.png`, fullPage: false });
  console.log('  Saved parent-journey-05-fees.png');

  // Screenshot 6: Record Payment modal
  console.log('Screenshot 6: Record Payment modal...');
  try {
    const recordPaymentBtn = await page.locator('button:has-text("Record Payment"), button:has-text("Make Payment"), button:has-text("Pay"), [data-testid="record-payment"]').first();
    if (await recordPaymentBtn.isVisible()) {
      await recordPaymentBtn.click();
      await page.waitForTimeout(1000);
    }
  } catch (e) {
    console.log('  Record Payment button not found, taking screenshot of current state');
  }
  await page.screenshot({ path: `${SCREENSHOTS_DIR}/parent-journey-06-payment-modal.png`, fullPage: false });
  console.log('  Saved parent-journey-06-payment-modal.png');

  await page.keyboard.press('Escape');
  await page.waitForTimeout(500);

  // Screenshot 7: Psychometric page
  console.log('Screenshot 7: Psychometric page...');
  await page.goto(`${BASE_URL}/parent/psychometric`);
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(3000);
  await page.screenshot({ path: `${SCREENSHOTS_DIR}/parent-journey-07-psychometric.png`, fullPage: false });
  console.log('  Saved parent-journey-07-psychometric.png');

  // Screenshot 8: Question Bank
  console.log('Screenshot 8: Question Bank...');
  await page.goto(`${BASE_URL}/parent/question-bank`);
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(3000);
  await page.screenshot({ path: `${SCREENSHOTS_DIR}/parent-journey-08-question-bank.png`, fullPage: false });
  console.log('  Saved parent-journey-08-question-bank.png (before generate)');

  // Try clicking "Generate Questions" if it exists
  try {
    const generateBtn = await page.locator('button:has-text("Generate Questions"), button:has-text("Generate"), [data-testid="generate-questions"]').first();
    if (await generateBtn.isVisible({ timeout: 2000 })) {
      console.log('  Found Generate Questions button, clicking...');
      await generateBtn.click();
      await page.waitForTimeout(2000);
      await page.screenshot({ path: `${SCREENSHOTS_DIR}/parent-journey-08-question-bank.png`, fullPage: false });
      console.log('  Updated parent-journey-08-question-bank.png (after generate click)');
    }
  } catch (e) {
    console.log('  Generate Questions button not found');
  }

  // Screenshot 9: Copilot
  console.log('Screenshot 9: Copilot...');
  await page.goto(`${BASE_URL}/parent/copilot`);
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(3000);
  await page.screenshot({ path: `${SCREENSHOTS_DIR}/parent-journey-09-copilot.png`, fullPage: false });
  console.log('  Saved parent-journey-09-copilot.png');

  // Screenshot 10: Profile
  console.log('Screenshot 10: Profile...');
  await page.goto(`${BASE_URL}/parent/profile`);
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(3000);
  await page.screenshot({ path: `${SCREENSHOTS_DIR}/parent-journey-10-profile.png`, fullPage: false });
  console.log('  Saved parent-journey-10-profile.png');

  await browser.close();
  console.log('\nParent Portal E2E Journey Complete!');
  console.log('Screenshots saved:');
  [
    'parent-journey-01-login.png',
    'parent-journey-02-dashboard.png',
    'parent-journey-03-children.png',
    'parent-journey-04-link-modal.png',
    'parent-journey-05-fees.png',
    'parent-journey-06-payment-modal.png',
    'parent-journey-07-psychometric.png',
    'parent-journey-08-question-bank.png',
    'parent-journey-09-copilot.png',
    'parent-journey-10-profile.png',
  ].forEach(f => console.log(`  ${SCREENSHOTS_DIR}/${f}`));
}

main().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
