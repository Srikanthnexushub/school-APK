const { chromium } = require('/Users/srikanth/IdeaProjects/school-APK/frontend/web/node_modules/playwright');
const path = require('path');
const fs = require('fs');

const SCREENSHOTS_DIR = '/tmp/studentD-screenshots';
if (!fs.existsSync(SCREENSHOTS_DIR)) fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });

async function screenshot(page, name) {
  const p = path.join(SCREENSHOTS_DIR, `${name}.png`);
  await page.screenshot({ path: p, fullPage: true });
  console.log(`SCREENSHOT: ${p}`);
}

async function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

async function getPageInfo(page) {
  const info = await page.evaluate(() => {
    const inputs = Array.from(document.querySelectorAll('input')).map(e => ({
      type: e.type, name: e.name, placeholder: e.placeholder, id: e.id, value: e.value
    }));
    const selects = Array.from(document.querySelectorAll('select')).map(e => ({
      name: e.name, id: e.id,
      options: Array.from(e.options).map(o => ({ value: o.value, text: o.text }))
    }));
    const buttons = Array.from(document.querySelectorAll('button')).map(e => ({
      type: e.type, text: e.textContent.trim().substring(0, 60), disabled: e.disabled
    }));
    return { inputs, selects, buttons, text: document.body.innerText.substring(0, 800) };
  });
  return info;
}

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await context.newPage();

  page.on('console', msg => {
    if (msg.type() === 'error') console.log('BROWSER ERROR:', msg.text());
  });

  try {
    // ===== STEP 1: Navigate to home page =====
    console.log('\n=== STEP 1: Navigate to http://13.126.138.9 ===');
    await page.goto('http://13.126.138.9', { waitUntil: 'networkidle', timeout: 30000 });
    await sleep(2000);
    console.log('URL:', page.url());
    console.log('Title:', await page.title());
    await screenshot(page, '01-homepage-login');

    // ===== STEP 2: Click Register link =====
    console.log('\n=== STEP 2: Click Register / Create Account ===');
    const registerLink = await page.$('a[href*="register"]');
    if (registerLink) {
      console.log('Found register link, clicking...');
      await registerLink.click();
    } else {
      console.log('Navigating directly to /register');
      await page.goto('http://13.126.138.9/register', { waitUntil: 'networkidle', timeout: 15000 });
    }
    await sleep(2000);
    console.log('URL:', page.url());
    await screenshot(page, '02-register-page');

    let info = await getPageInfo(page);
    console.log('Page text:', info.text);
    console.log('Selects:', JSON.stringify(info.selects));
    console.log('Buttons:', JSON.stringify(info.buttons));

    // ===== STEP 3: Select STUDENT role from dropdown =====
    console.log('\n=== STEP 3: Select STUDENT role ===');
    const selectEl = await page.$('select');
    if (selectEl) {
      await selectEl.selectOption('STUDENT');
      console.log('Selected STUDENT from role dropdown');
      await sleep(1000);
    }
    await screenshot(page, '03-role-selected');

    // ===== STEP 4: Click Next/Create Account =====
    console.log('\n=== STEP 4: Click Next/Create Account button ===');
    info = await getPageInfo(page);
    console.log('Buttons:', JSON.stringify(info.buttons));

    // Find the next/create account button (not Back)
    const allButtons = await page.$$('button');
    for (const btn of allButtons) {
      const text = await btn.textContent();
      const trimmed = text.trim();
      if (trimmed && !trimmed.toLowerCase().includes('back') && !trimmed.toLowerCase().includes('sign in')) {
        console.log(`Clicking button: "${trimmed}"`);
        await btn.click();
        await sleep(2000);
        break;
      }
    }

    console.log('URL after step 1 next:', page.url());
    await screenshot(page, '04-after-role-next');

    info = await getPageInfo(page);
    console.log('Page text:', info.text);
    console.log('Inputs:', JSON.stringify(info.inputs));

    // ===== STEP 5: Fill personal details (Step 2 of form) =====
    console.log('\n=== STEP 5: Fill personal/academic details ===');

    // Fill by placeholder or name
    async function fillInput(selectors, value, label) {
      for (const sel of selectors) {
        try {
          const el = await page.$(sel);
          if (el) {
            await el.clear();
            await el.fill(value);
            console.log(`Filled ${label} with selector: ${sel}`);
            return true;
          }
        } catch(e) {}
      }
      console.log(`WARNING: Could not fill ${label}`);
      return false;
    }

    await fillInput([
      'input[name="firstName"]', 'input[placeholder*="First" i]',
      'input[placeholder*="first" i]', '#firstName'
    ], 'Student', 'firstName');

    await fillInput([
      'input[name="lastName"]', 'input[placeholder*="Last" i]',
      'input[placeholder*="last" i]', '#lastName'
    ], 'D', 'lastName');

    await fillInput([
      'input[name="email"]', 'input[type="email"]',
      'input[placeholder*="email" i]', '#email'
    ], 'studentD@school.com', 'email');

    // Password fields
    const passInputs = await page.$$('input[type="password"]');
    console.log(`Found ${passInputs.length} password fields`);
    if (passInputs.length >= 1) {
      await passInputs[0].fill('Test@12345');
      console.log('Filled password[0]');
    }
    if (passInputs.length >= 2) {
      await passInputs[1].fill('Test@12345');
      console.log('Filled password[1] (confirm)');
    }

    await screenshot(page, '05-personal-details-filled');

    // Look for Next button
    info = await getPageInfo(page);
    console.log('Buttons after filling:', JSON.stringify(info.buttons));

    const allBtns2 = await page.$$('button');
    for (const btn of allBtns2) {
      const text = (await btn.textContent()).trim();
      if (text && !text.toLowerCase().includes('back') && !text.toLowerCase().includes('sign in')) {
        const disabled = await btn.evaluate(el => el.disabled);
        console.log(`Button "${text}" disabled: ${disabled}`);
        if (!disabled) {
          console.log(`Clicking: "${text}"`);
          await btn.click();
          await sleep(2000);
          break;
        }
      }
    }

    console.log('URL after personal details:', page.url());
    await screenshot(page, '06-after-personal-next');

    info = await getPageInfo(page);
    console.log('Page text:', info.text);
    console.log('Inputs:', JSON.stringify(info.inputs));
    console.log('Selects:', JSON.stringify(info.selects));

    // ===== STEP 6: Fill academic details / phone / DOB / center code =====
    console.log('\n=== STEP 6: Fill academic/contact details ===');

    await fillInput([
      'input[name="phone"]', 'input[name="phoneNumber"]', 'input[type="tel"]',
      'input[placeholder*="phone" i]', 'input[placeholder*="mobile" i]'
    ], '9876543210', 'phone');

    await fillInput([
      'input[name="dateOfBirth"]', 'input[name="dob"]', 'input[type="date"]',
      'input[placeholder*="birth" i]', 'input[placeholder*="DOB" i]'
    ], '2005-06-15', 'dateOfBirth');

    // Center/institution code - try various names
    await fillInput([
      'input[name="centerCode"]', 'input[name="institutionCode"]', 'input[name="center_code"]',
      'input[placeholder*="center" i]', 'input[placeholder*="institution" i]',
      'input[placeholder*="code" i]', 'input[placeholder*="school" i]'
    ], 'nexus', 'centerCode');

    // Check for grade/class select
    const selectEls = await page.$$('select');
    for (const sel of selectEls) {
      const opts = await sel.$$eval('option', opts => opts.map(o => ({ value: o.value, text: o.text })));
      console.log('Select options:', JSON.stringify(opts));
      // If looks like grade selection, pick something
      if (opts.some(o => o.text.match(/grade|class|standard/i))) {
        await sel.selectOption({ index: 1 });
        console.log('Selected first grade option');
      }
    }

    await screenshot(page, '07-academic-details-filled');
    await sleep(1000);

    // ===== STEP 7: Check for CAPTCHA =====
    console.log('\n=== STEP 7: Check for CAPTCHA ===');
    const hasCaptcha = await page.evaluate(() => {
      const canvas = document.querySelector('canvas');
      const captchaDiv = document.querySelector('[class*="captcha"], [id*="captcha"]');
      return { hasCanvas: !!canvas, hasCaptchaDiv: !!captchaDiv };
    });
    console.log('CAPTCHA check:', hasCaptcha);

    if (hasCaptcha.hasCanvas) {
      console.log('CAPTCHA FOUND - saving canvas image');
      await screenshot(page, '08-captcha-visible');

      // Try to read canvas image data
      const canvasData = await page.evaluate(() => {
        const canvas = document.querySelector('canvas');
        return canvas ? canvas.toDataURL('image/png') : null;
      });
      if (canvasData) {
        const base64 = canvasData.replace(/^data:image\/png;base64,/, '');
        fs.writeFileSync(path.join(SCREENSHOTS_DIR, '08-captcha-canvas.png'), base64, 'base64');
        console.log('Saved captcha canvas image');
      }

      // Look for captcha input
      const captchaInput = await page.$('input[name*="captcha" i], input[placeholder*="captcha" i], input[id*="captcha" i]');
      if (captchaInput) {
        console.log('Found captcha input field - manual reading needed');
        // Note: We cannot auto-read the CAPTCHA text; report it to user
      }
    }

    // ===== STEP 8: Submit =====
    console.log('\n=== STEP 8: Click Submit/Register button ===');
    info = await getPageInfo(page);
    console.log('All buttons:', JSON.stringify(info.buttons));

    const submitBtns = await page.$$('button');
    for (const btn of submitBtns) {
      const text = (await btn.textContent()).trim();
      const disabled = await btn.evaluate(el => el.disabled);
      console.log(`  Button: "${text}" disabled=${disabled}`);
      if (!disabled && text && !text.toLowerCase().includes('back')) {
        console.log(`Clicking submit: "${text}"`);
        await btn.click();
        await sleep(3000);
        console.log('URL after submit:', page.url());
        await screenshot(page, '09-after-submit');
        break;
      }
    }

    // ===== STEP 9: Post-submission state =====
    console.log('\n=== STEP 9: Post-submission state ===');
    const finalUrl = page.url();
    console.log('Current URL:', finalUrl);
    info = await getPageInfo(page);
    console.log('Page text:', info.text);
    await screenshot(page, '10-post-submit');

    // If there is an OTP step
    if (info.text.toLowerCase().includes('otp') || info.text.toLowerCase().includes('verify')) {
      console.log('OTP/VERIFY step detected!');
      await screenshot(page, '10b-otp-step');
    }

    // If redirected to login
    if (finalUrl.includes('login') || info.inputs.some(i => i.type === 'email' || i.type === 'password')) {
      console.log('\n=== STEP 10: Redirected to login — logging in ===');
      await fillInput(['input[type="email"]', 'input[name="email"]'], 'studentD@school.com', 'login email');
      const pwFields = await page.$$('input[type="password"]');
      if (pwFields.length > 0) {
        await pwFields[0].fill('Test@12345');
        console.log('Filled login password');
      }
      await screenshot(page, '11-login-filled');

      // Check for CAPTCHA on login
      const loginCanvas = await page.$('canvas');
      if (loginCanvas) {
        console.log('CAPTCHA on login page!');
        const canvasData = await page.evaluate(() => {
          const c = document.querySelector('canvas');
          return c ? c.toDataURL() : null;
        });
        if (canvasData) {
          const b64 = canvasData.replace(/^data:image\/png;base64,/, '');
          fs.writeFileSync(path.join(SCREENSHOTS_DIR, '11-login-captcha.png'), b64, 'base64');
          console.log('Saved login CAPTCHA canvas');
        }
        const captchaInput = await page.$('input[name*="captcha" i], input[placeholder*="captcha" i]');
        if (captchaInput) {
          console.log('Login has CAPTCHA input - need to solve manually');
        }
      }

      // Submit login
      const loginBtn = await page.$('button[type="submit"]');
      if (loginBtn) {
        await loginBtn.click();
        await sleep(4000);
        console.log('URL after login:', page.url());
        await screenshot(page, '12-after-login');
      }
    }

    // ===== FINAL STATE =====
    await sleep(2000);
    console.log('\n=== FINAL STATE ===');
    console.log('Final URL:', page.url());
    info = await getPageInfo(page);
    console.log('Final page text:', info.text);
    await screenshot(page, '13-final-state');

  } catch (err) {
    console.error('FATAL ERROR:', err.message);
    console.error(err.stack);
    try {
      await screenshot(page, 'fatal-error-state');
    } catch(e2) {}
  } finally {
    await browser.close();
    console.log('\n=== Screenshots saved to:', SCREENSHOTS_DIR, '===');
    console.log('Files:', fs.readdirSync(SCREENSHOTS_DIR).join(', '));
  }
}

main();
