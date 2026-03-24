const { chromium } = require('playwright');
const path = require('path');
const https = require('https');
const http = require('http');

const OUT = '/Users/srikanth/IdeaProjects/school-APK';

const STUDENT_EMAIL = 'student_1773998793@nexused-demo.edu';
const STUDENT_PASS = 'Student@2026!';
const PARENT_EMAIL = 'parent_1773998793@nexused-demo.edu';
const PARENT_PASS = 'Parent@2026!';
const CA_EMAIL = 'ca_1773998793@nexused-demo.edu';
const CA_PASS = 'Demo@2026!';

function fetchJson(url, body) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(body);
    const req = http.request(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data) }
    }, (res) => {
      let raw = '';
      res.on('data', c => raw += c);
      res.on('end', () => { try { resolve(JSON.parse(raw)); } catch(e) { reject(e); } });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

async function loginAndInject(page, email, password) {
  const resp = await fetchJson('http://localhost:8180/api/v1/auth/login', {
    email, password,
    captchaToken: 'E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD:bypass',
    deviceFingerprint: {userAgent: 'E2E-Test/1.0', deviceId: 'e2e-device-001', ipSubnet: '127.0.0.1/24'}
  });

  console.log(`Login response for ${email}:`, Object.keys(resp || {}));
  if (!resp || !resp.accessToken) {
    console.error('Login failed:', JSON.stringify(resp));
    return null;
  }

  const jwtPayload = JSON.parse(Buffer.from(resp.accessToken.split('.')[1], 'base64').toString());
  console.log(`JWT payload:`, JSON.stringify(jwtPayload));

  await page.evaluate(({token, refreshToken, jwtPayload}) => {
    localStorage.setItem('edupath-auth', JSON.stringify({
      state: {
        token: token,
        refreshToken: refreshToken,
        deviceId: 'e2e-device-001',
        user: {
          id: jwtPayload.sub,
          email: jwtPayload.email,
          role: jwtPayload.role,
          centerId: jwtPayload.centerId
        },
        isAuthenticated: true
      },
      version: 0
    }));
  }, {token: resp.accessToken, refreshToken: resp.refreshToken, jwtPayload});

  return jwtPayload;
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  await page.setViewportSize({ width: 1400, height: 900 });

  // === STEP 1: Student → /assignments ===
  console.log('\n--- Step 1: Student assignments ---');
  await page.goto('http://localhost:3000');
  await page.waitForTimeout(2000);
  const studentJwt = await loginAndInject(page, STUDENT_EMAIL, STUDENT_PASS);
  if (!studentJwt) { console.error('Student login failed'); }
  await page.reload();
  await page.waitForTimeout(2000);
  await page.goto('http://localhost:3000/assignments');
  await page.waitForTimeout(3000);
  await page.screenshot({ path: path.join(OUT, 'e2e-final-01-student-assignments.png'), fullPage: false });
  console.log('Screenshot 1 taken');
  // Print page text for verification
  const p1text = await page.locator('body').textContent();
  console.log('Page 1 text (first 500):', p1text.substring(0, 500));

  // === STEP 2: Click "Graded" filter tab ===
  console.log('\n--- Step 2: Graded filter ---');
  const allButtons = await page.locator('button').all();
  for (const btn of allButtons) {
    const txt = (await btn.textContent() || '').trim();
    if (txt) console.log('  Button:', txt);
  }
  const gradedBtn = page.locator('button', {hasText: /^graded$/i}).first();
  const gradedCount = await gradedBtn.count();
  console.log('Graded button count:', gradedCount);
  if (gradedCount > 0) {
    await gradedBtn.click();
    console.log('Clicked Graded button');
  } else {
    // try any element with "Graded" text
    const anyGraded = page.locator('text=Graded').first();
    if (await anyGraded.count() > 0) {
      await anyGraded.click();
      console.log('Clicked any Graded element');
    }
  }
  await page.waitForTimeout(2500);
  await page.screenshot({ path: path.join(OUT, 'e2e-final-02-student-assignments-graded.png'), fullPage: false });
  console.log('Screenshot 2 taken');

  // === STEP 3: Parent dashboard ===
  console.log('\n--- Step 3: Parent dashboard ---');
  await page.goto('http://localhost:3000');
  await page.waitForTimeout(2000);
  await loginAndInject(page, PARENT_EMAIL, PARENT_PASS);
  await page.reload();
  await page.waitForTimeout(2000);
  await page.goto('http://localhost:3000');
  await page.waitForTimeout(3000);
  await page.screenshot({ path: path.join(OUT, 'e2e-final-03-parent-dashboard.png'), fullPage: false });
  console.log('Screenshot 3 taken');
  const p3text = await page.locator('body').textContent();
  console.log('Page 3 text (first 500):', p3text.substring(0, 500));

  // === STEP 4: /parent/children ===
  console.log('\n--- Step 4: Parent children ---');
  await page.goto('http://localhost:3000/parent/children');
  await page.waitForTimeout(3000);
  await page.screenshot({ path: path.join(OUT, 'e2e-final-04-parent-children.png'), fullPage: false });
  console.log('Screenshot 4 taken');
  const p4text = await page.locator('body').textContent();
  console.log('Page 4 text (first 500):', p4text.substring(0, 500));

  // === STEP 5: CENTER_ADMIN overview ===
  console.log('\n--- Step 5: CENTER_ADMIN overview ---');
  await page.goto('http://localhost:3000');
  await page.waitForTimeout(2000);
  await loginAndInject(page, CA_EMAIL, CA_PASS);
  await page.reload();
  await page.waitForTimeout(2000);
  await page.goto('http://localhost:3000/admin');
  await page.waitForTimeout(4000);
  await page.screenshot({ path: path.join(OUT, 'e2e-final-05-admin-overview.png'), fullPage: false });
  console.log('Screenshot 5 taken');
  const p5text = await page.locator('body').textContent();
  console.log('Page 5 text (first 500):', p5text.substring(0, 500));

  // === STEP 6: /admin?tab=assignments ===
  console.log('\n--- Step 6: Admin assignments tab ---');
  await page.goto('http://localhost:3000/admin?tab=assignments');
  await page.waitForTimeout(3500);
  await page.screenshot({ path: path.join(OUT, 'e2e-final-06-admin-assignments.png'), fullPage: false });
  console.log('Screenshot 6 taken');
  const p6text = await page.locator('body').textContent();
  console.log('Page 6 text (first 500):', p6text.substring(0, 500));

  // === STEP 7: /admin?tab=batches ===
  console.log('\n--- Step 7: Admin batches tab ---');
  await page.goto('http://localhost:3000/admin?tab=batches');
  await page.waitForTimeout(3500);
  await page.screenshot({ path: path.join(OUT, 'e2e-final-07-admin-batches.png'), fullPage: false });
  console.log('Screenshot 7 taken');
  const p7text = await page.locator('body').textContent();
  console.log('Page 7 text (first 500):', p7text.substring(0, 500));

  await browser.close();
  console.log('\nAll done!');
})().catch(err => { console.error('FATAL:', err.stack || err); process.exit(1); });
