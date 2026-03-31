const { chromium } = require('/Users/srikanth/IdeaProjects/school-APK/frontend/web/node_modules/playwright');
const path = require('path');
const fs = require('fs');

const SCREENSHOTS_DIR = '/tmp/studentD-screenshots';
if (!fs.existsSync(SCREENSHOTS_DIR)) fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });

let stepNum = 1;
async function screenshot(page, label) {
  const name = `${String(stepNum).padStart(2,'0')}-${label}`;
  stepNum++;
  const p = path.join(SCREENSHOTS_DIR, `${name}.png`);
  await page.screenshot({ path: p, fullPage: true });
  console.log(`  [screenshot] ${p}`);
  return p;
}

async function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await context.newPage();

  // Log page errors
  page.on('pageerror', err => console.log('PAGE ERROR:', err.message));

  try {
    // ==========================================
    // STEP 1: Homepage (login page)
    // ==========================================
    console.log('\n--- STEP 1: Navigate to http://13.126.138.9 ---');
    await page.goto('http://13.126.138.9', { waitUntil: 'networkidle', timeout: 30000 });
    await sleep(1500);
    console.log('  URL:', page.url());
    console.log('  Title:', await page.title());
    await screenshot(page, 'homepage-login');

    // ==========================================
    // STEP 2: Click "Create one" register link
    // ==========================================
    console.log('\n--- STEP 2: Click Register link ---');
    await page.locator('a[href*="register"]').first().click();
    await sleep(2000);
    console.log('  URL:', page.url());
    await screenshot(page, 'register-step1-role');

    const pageText = await page.evaluate(() => document.body.innerText.substring(0, 300));
    console.log('  Page:', pageText);

    // ==========================================
    // STEP 3: Select STUDENT role
    // ==========================================
    console.log('\n--- STEP 3: Select STUDENT role ---');
    await page.locator('select').first().selectOption('STUDENT');
    await sleep(500);
    console.log('  Selected STUDENT');
    await screenshot(page, 'role-student-selected');

    // Now the form should expand - get full list of inputs
    const allInputs = await page.evaluate(() => {
      return Array.from(document.querySelectorAll('input')).map(e => ({
        type: e.type, name: e.name, placeholder: e.placeholder
      }));
    });
    console.log('  Inputs after role select:', JSON.stringify(allInputs));

    // ==========================================
    // STEP 4: Fill all required fields
    // ==========================================
    console.log('\n--- STEP 4: Fill registration form fields ---');

    // First Name
    await page.locator('input[name="firstName"]').fill('Student');
    console.log('  Filled firstName = Student');

    // Last Name
    await page.locator('input[name="lastName"]').fill('D');
    console.log('  Filled lastName = D');

    // Phone (optional but fill it)
    await page.locator('input[name="phone"]').fill('9876543210');
    console.log('  Filled phone = 9876543210');

    // Date of Birth
    await page.locator('input[name="dateOfBirth"]').fill('2005-06-15');
    console.log('  Filled dateOfBirth = 2005-06-15');

    // Email
    await page.locator('input[name="email"]').fill('studentD@school.com');
    console.log('  Filled email = studentD@school.com');

    // Password
    await page.locator('input[name="password"]').fill('Test@12345');
    console.log('  Filled password');

    // Confirm Password
    await page.locator('input[name="confirmPassword"]').fill('Test@12345');
    console.log('  Filled confirmPassword');

    await sleep(500);
    await screenshot(page, 'form-personal-filled');

    // ==========================================
    // STEP 5: Handle CAPTCHA
    // ==========================================
    console.log('\n--- STEP 5: Check for CAPTCHA ---');

    // Look for canvas element (CAPTCHA image)
    const captchaCanvas = await page.$('canvas');
    let captchaText = null;

    if (captchaCanvas) {
      console.log('  CAPTCHA canvas found!');
      await screenshot(page, 'captcha-visible');

      // Save CAPTCHA image
      const canvasData = await page.evaluate(() => {
        const c = document.querySelector('canvas');
        return c ? c.toDataURL('image/png') : null;
      });
      if (canvasData) {
        const b64 = canvasData.replace(/^data:image\/png;base64,/, '');
        const captchaPath = path.join(SCREENSHOTS_DIR, 'captcha-image.png');
        fs.writeFileSync(captchaPath, b64, 'base64');
        console.log('  CAPTCHA image saved:', captchaPath);
      }

      // Try to get captcha text via aria or alt
      const captchaInfo = await page.evaluate(() => {
        const canvas = document.querySelector('canvas');
        const parent = canvas?.parentElement;
        return {
          ariaLabel: canvas?.getAttribute('aria-label'),
          title: canvas?.title,
          parentText: parent?.innerText,
          canvasWidth: canvas?.width,
          canvasHeight: canvas?.height
        };
      });
      console.log('  CAPTCHA info:', captchaInfo);

      // Find CAPTCHA input field
      const captchaInputs = await page.evaluate(() => {
        return Array.from(document.querySelectorAll('input')).map(e => ({
          type: e.type, name: e.name, placeholder: e.placeholder, id: e.id
        }));
      });
      console.log('  All inputs (including captcha):', JSON.stringify(captchaInputs));

    } else {
      console.log('  No canvas CAPTCHA found');
      // Check for text-based captcha or other security check
      const securityCheck = await page.evaluate(() => {
        const text = document.body.innerText;
        return text.includes('security') || text.includes('captcha') || text.includes('Security check');
      });
      if (securityCheck) {
        console.log('  Security check text found in page');
        await screenshot(page, 'security-check');
      }
    }

    await screenshot(page, 'before-submit');

    // Log the CAPTCHA area specifically
    const captchaArea = await page.evaluate(() => {
      const inputs = Array.from(document.querySelectorAll('input'));
      const captchaInput = inputs.find(i => i.placeholder && i.placeholder.toLowerCase().includes('character'));
      return captchaInput ? {
        placeholder: captchaInput.placeholder,
        name: captchaInput.name,
        id: captchaInput.id
      } : null;
    });
    console.log('  Captcha input field:', captchaArea);

    // ==========================================
    // STEP 6: Try to submit (may fail due to CAPTCHA)
    // ==========================================
    console.log('\n--- STEP 6: Submit registration ---');

    // Check button state
    const btnInfo = await page.evaluate(() => {
      const btn = document.querySelector('button[type="submit"]');
      return btn ? { text: btn.textContent.trim(), disabled: btn.disabled } : null;
    });
    console.log('  Submit button:', btnInfo);

    if (btnInfo && !btnInfo.disabled) {
      await page.locator('button[type="submit"]').click();
      await sleep(3000);
    } else {
      console.log('  Submit button disabled — trying to force click');
      // Check what validation is missing
      const validationErrors = await page.evaluate(() => {
        const errors = Array.from(document.querySelectorAll('[class*="error"], [class*="invalid"], [aria-invalid="true"]'));
        return errors.map(e => ({ text: e.textContent.trim().substring(0, 100), class: e.className.substring(0, 60) }));
      });
      console.log('  Validation errors:', JSON.stringify(validationErrors));

      // Get all visible text to understand what's required
      const fullText = await page.evaluate(() => document.body.innerText);
      console.log('  Full page text:', fullText.substring(0, 1500));
      await screenshot(page, 'validation-errors');
    }

    console.log('  URL after submit attempt:', page.url());
    await screenshot(page, 'after-submit');

    // ==========================================
    // STEP 7: Check post-submission result
    // ==========================================
    console.log('\n--- STEP 7: Post-submission state ---');
    const postUrl = page.url();
    const postText = await page.evaluate(() => document.body.innerText.substring(0, 800));
    console.log('  URL:', postUrl);
    console.log('  Text:', postText);
    await screenshot(page, 'post-submit-state');

    // Check for OTP
    if (postText.toLowerCase().includes('otp') || postText.toLowerCase().includes('verification')) {
      console.log('  OTP/VERIFICATION STEP DETECTED');
    }

    // Check for success message
    if (postText.toLowerCase().includes('success') || postText.toLowerCase().includes('welcome') || postText.toLowerCase().includes('dashboard')) {
      console.log('  SUCCESS - registration appears complete');
    }

    // ==========================================
    // If still on register page, check what's wrong
    // ==========================================
    if (postUrl.includes('register')) {
      console.log('\n--- Still on register page - checking captcha ---');
      // Read the CAPTCHA canvas pixel data to try to extract text
      const captchaData = await page.evaluate(() => {
        const canvas = document.querySelector('canvas');
        if (!canvas) return null;
        try {
          const ctx = canvas.getContext('2d');
          const w = canvas.width;
          const h = canvas.height;
          return {
            width: w,
            height: h,
            dataUrl: canvas.toDataURL()
          };
        } catch(e) {
          return { error: e.message };
        }
      });
      console.log('  CAPTCHA data:', captchaData ? `${captchaData.width}x${captchaData.height}` : 'none');

      if (captchaData && captchaData.dataUrl) {
        const b64 = captchaData.dataUrl.replace(/^data:image\/png;base64,/, '');
        const p = path.join(SCREENSHOTS_DIR, 'captcha-final.png');
        fs.writeFileSync(p, b64, 'base64');
        console.log('  Saved captcha to:', p);
      }

      // Full screenshot showing what's on the page
      await screenshot(page, 'still-on-register');
      const fullText2 = await page.evaluate(() => document.body.innerText);
      console.log('  Full page (register):', fullText2.substring(0, 2000));
    }

    // ==========================================
    // If on login page, login
    // ==========================================
    if (postUrl.includes('login')) {
      console.log('\n--- STEP 8: On login page — attempting login ---');

      // Check for CAPTCHA on login
      const loginCanvas = await page.$('canvas');
      if (loginCanvas) {
        console.log('  Login page has CAPTCHA canvas');
        const canvasData = await page.evaluate(() => {
          const c = document.querySelector('canvas');
          return c ? c.toDataURL() : null;
        });
        if (canvasData) {
          const b64 = canvasData.replace(/^data:image\/png;base64,/, '');
          fs.writeFileSync(path.join(SCREENSHOTS_DIR, 'login-captcha.png'), b64, 'base64');
          console.log('  Login CAPTCHA saved');
        }
      }

      await page.locator('input[type="email"], input[name="email"]').first().fill('studentD@school.com');
      console.log('  Filled login email');

      const loginPwFields = await page.$$('input[type="password"]');
      if (loginPwFields.length > 0) {
        await loginPwFields[0].fill('Test@12345');
        console.log('  Filled login password');
      }

      await screenshot(page, 'login-form-filled');

      // Submit login form
      const loginSubmit = await page.$('button[type="submit"]');
      if (loginSubmit) {
        const disabled = await loginSubmit.evaluate(el => el.disabled);
        if (!disabled) {
          await loginSubmit.click();
          await sleep(4000);
          console.log('  URL after login:', page.url());
          await screenshot(page, 'after-login');
        } else {
          console.log('  Login submit disabled - CAPTCHA required');
          await screenshot(page, 'login-captcha-required');
        }
      }
    }

    // ==========================================
    // FINAL STATE
    // ==========================================
    await sleep(2000);
    console.log('\n--- FINAL STATE ---');
    console.log('  URL:', page.url());
    const finalText = await page.evaluate(() => document.body.innerText.substring(0, 600));
    console.log('  Text:', finalText);
    await screenshot(page, 'final-state');

  } catch (err) {
    console.error('FATAL ERROR:', err.message);
    try { await screenshot(page, 'error'); } catch(e) {}
  } finally {
    await browser.close();
    console.log('\n=== Done. Screenshots in:', SCREENSHOTS_DIR);
    const files = fs.readdirSync(SCREENSHOTS_DIR).sort();
    console.log('Files:');
    files.forEach(f => console.log('  ', path.join(SCREENSHOTS_DIR, f)));
  }
}

main();
