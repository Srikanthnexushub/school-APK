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

async function main() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await context.newPage();

  // Collect console errors
  page.on('console', msg => {
    if (msg.type() === 'error') console.log('BROWSER ERROR:', msg.text());
  });

  try {
    // Step 1: Navigate to home page
    console.log('\n=== STEP 1: Navigate to http://13.126.138.9 ===');
    await page.goto('http://13.126.138.9', { waitUntil: 'networkidle', timeout: 30000 });
    await sleep(2000);
    console.log('URL:', page.url());
    console.log('Title:', await page.title());
    await screenshot(page, '01-homepage');

    // Step 2: Find and click Register/Sign Up
    console.log('\n=== STEP 2: Click Register / Sign Up ===');
    // Look for register link
    const registerSelectors = [
      'text=Register',
      'text=Sign Up',
      'text=Create Account',
      'a[href*="register"]',
      'button:has-text("Register")',
      'button:has-text("Sign Up")',
      '[data-testid="register"]',
      'a:has-text("Register")',
    ];

    let registerClicked = false;
    for (const sel of registerSelectors) {
      try {
        const el = await page.$(sel);
        if (el) {
          console.log(`Found register element with selector: ${sel}`);
          const text = await el.textContent();
          console.log(`Element text: ${text}`);
          await el.click();
          registerClicked = true;
          break;
        }
      } catch(e) {}
    }

    if (!registerClicked) {
      // Try navigating directly
      console.log('Register button not found, trying direct navigation to /register');
      await page.goto('http://13.126.138.9/register', { waitUntil: 'networkidle', timeout: 15000 });
    }

    await sleep(2000);
    console.log('URL after register click:', page.url());
    await screenshot(page, '02-register-page');

    // Log visible text on page
    const bodyText = await page.evaluate(() => document.body.innerText.substring(0, 500));
    console.log('Page text (first 500):', bodyText);

    // Step 3: Fill registration form - First Name
    console.log('\n=== STEP 3: Fill registration form ===');

    // Check what inputs are available
    const inputs = await page.$$eval('input', els => els.map(e => ({
      type: e.type,
      name: e.name,
      placeholder: e.placeholder,
      id: e.id,
      'aria-label': e.getAttribute('aria-label')
    })));
    console.log('Available inputs:', JSON.stringify(inputs, null, 2));

    // Try to fill First Name
    const firstNameSelectors = [
      'input[name="firstName"]',
      'input[placeholder*="First"]',
      'input[placeholder*="first"]',
      '#firstName',
      'input[aria-label*="First"]',
    ];
    for (const sel of firstNameSelectors) {
      try {
        const el = await page.$(sel);
        if (el) {
          await el.fill('Student');
          console.log(`Filled firstName with selector: ${sel}`);
          break;
        }
      } catch(e) {}
    }

    // Fill Last Name
    const lastNameSelectors = [
      'input[name="lastName"]',
      'input[placeholder*="Last"]',
      'input[placeholder*="last"]',
      '#lastName',
      'input[aria-label*="Last"]',
    ];
    for (const sel of lastNameSelectors) {
      try {
        const el = await page.$(sel);
        if (el) {
          await el.fill('D');
          console.log(`Filled lastName with selector: ${sel}`);
          break;
        }
      } catch(e) {}
    }

    // Fill Email
    const emailSelectors = [
      'input[name="email"]',
      'input[type="email"]',
      'input[placeholder*="email"]',
      'input[placeholder*="Email"]',
      '#email',
    ];
    for (const sel of emailSelectors) {
      try {
        const el = await page.$(sel);
        if (el) {
          await el.fill('studentD@school.com');
          console.log(`Filled email with selector: ${sel}`);
          break;
        }
      } catch(e) {}
    }

    // Fill Password
    const passwordSelectors = [
      'input[name="password"]',
      'input[type="password"]',
      '#password',
    ];
    let passwordFilled = 0;
    for (const sel of passwordSelectors) {
      try {
        const els = await page.$$(sel);
        if (els.length > 0 && passwordFilled < els.length) {
          await els[passwordFilled].fill('Test@12345');
          console.log(`Filled password[${passwordFilled}] with selector: ${sel}`);
          passwordFilled++;
          if (passwordFilled >= 2) break; // fill password and confirm
        }
      } catch(e) {}
    }

    // If only one password field found, look for confirm
    if (passwordFilled < 2) {
      const confirmSelectors = [
        'input[name="confirmPassword"]',
        'input[name="confirm_password"]',
        'input[placeholder*="confirm"]',
        'input[placeholder*="Confirm"]',
        '#confirmPassword',
      ];
      for (const sel of confirmSelectors) {
        try {
          const el = await page.$(sel);
          if (el) {
            await el.fill('Test@12345');
            console.log(`Filled confirmPassword with selector: ${sel}`);
            break;
          }
        } catch(e) {}
      }
    }

    await screenshot(page, '03-form-filled-basic');
    await sleep(1000);

    // Step 4: Select STUDENT role
    console.log('\n=== STEP 4: Select STUDENT role ===');

    // Look for role selection
    const roleSelectors = [
      'text=Student',
      '[data-value="STUDENT"]',
      'input[value="STUDENT"]',
      'button:has-text("Student")',
      '[role="option"]:has-text("Student")',
    ];

    for (const sel of roleSelectors) {
      try {
        const el = await page.$(sel);
        if (el) {
          await el.click();
          console.log(`Clicked student role with selector: ${sel}`);
          await sleep(1000);
          break;
        }
      } catch(e) {}
    }

    // Check for select element
    const selects = await page.$$eval('select', els => els.map(e => ({
      name: e.name,
      id: e.id,
      options: Array.from(e.options).map(o => ({ value: o.value, text: o.text }))
    })));
    console.log('Select elements:', JSON.stringify(selects, null, 2));

    for (const sel of selects) {
      if (sel.options.some(o => o.value === 'STUDENT' || o.text.toLowerCase().includes('student'))) {
        const selectEl = await page.$(`select[name="${sel.name}"]`) || await page.$(`select#${sel.id}`);
        if (selectEl) {
          await selectEl.selectOption({ label: sel.options.find(o => o.text.toLowerCase().includes('student'))?.text });
          console.log('Selected STUDENT from select element');
        }
      }
    }

    await screenshot(page, '04-role-selected');
    await sleep(1000);

    // Look for Next button to proceed to next step
    console.log('\n=== Checking for Next/Continue button ===');
    const nextSelectors = [
      'button:has-text("Next")',
      'button:has-text("Continue")',
      'button[type="submit"]',
      'button:has-text("Proceed")',
    ];

    for (const sel of nextSelectors) {
      try {
        const el = await page.$(sel);
        if (el) {
          const text = await el.textContent();
          console.log(`Found next button: "${text}" (${sel})`);
          await el.click();
          await sleep(2000);
          console.log('URL after next:', page.url());
          await screenshot(page, '05-after-next');
          break;
        }
      } catch(e) {}
    }

    // Step 5: Check if we're on step 2 - academic details
    console.log('\n=== STEP 5: Fill academic details ===');
    const inputs2 = await page.$$eval('input', els => els.map(e => ({
      type: e.type,
      name: e.name,
      placeholder: e.placeholder,
      id: e.id
    })));
    console.log('Inputs on current page:', JSON.stringify(inputs2, null, 2));

    // Fill phone
    const phoneSelectors = [
      'input[name="phone"]',
      'input[name="phoneNumber"]',
      'input[type="tel"]',
      'input[placeholder*="phone"]',
      'input[placeholder*="Phone"]',
      'input[placeholder*="mobile"]',
    ];
    for (const sel of phoneSelectors) {
      try {
        const el = await page.$(sel);
        if (el) {
          await el.fill('9876543210');
          console.log(`Filled phone with selector: ${sel}`);
          break;
        }
      } catch(e) {}
    }

    // Fill DOB
    const dobSelectors = [
      'input[name="dateOfBirth"]',
      'input[name="dob"]',
      'input[type="date"]',
      'input[placeholder*="DOB"]',
      'input[placeholder*="date"]',
      'input[placeholder*="birth"]',
    ];
    for (const sel of dobSelectors) {
      try {
        const el = await page.$(sel);
        if (el) {
          await el.fill('2005-06-15');
          console.log(`Filled DOB with selector: ${sel}`);
          break;
        }
      } catch(e) {}
    }

    // Fill institution/center code
    const centerSelectors = [
      'input[name="centerCode"]',
      'input[name="institutionCode"]',
      'input[name="center_code"]',
      'input[placeholder*="center"]',
      'input[placeholder*="Center"]',
      'input[placeholder*="institution"]',
      'input[placeholder*="Institution"]',
      'input[placeholder*="code"]',
      'input[placeholder*="Code"]',
    ];
    for (const sel of centerSelectors) {
      try {
        const el = await page.$(sel);
        if (el) {
          await el.fill('nexus');
          console.log(`Filled center code with selector: ${sel}`);
          break;
        }
      } catch(e) {}
    }

    await screenshot(page, '06-academic-details-filled');
    await sleep(1000);

    // Look for any CAPTCHA
    console.log('\n=== STEP 6: Check for CAPTCHA ===');
    const captchaVisible = await page.$('canvas, .captcha, [class*="captcha"], img[alt*="captcha"]');
    if (captchaVisible) {
      console.log('CAPTCHA element found!');
      const captchaScreenshot = path.join(SCREENSHOTS_DIR, '07-captcha.png');
      await page.screenshot({ path: captchaScreenshot, fullPage: true });
      console.log(`CAPTCHA SCREENSHOT: ${captchaScreenshot}`);

      // Try to read captcha text from page
      const captchaText = await page.evaluate(() => {
        const canvas = document.querySelector('canvas');
        if (canvas) return canvas.toDataURL();
        return null;
      });
      if (captchaText) {
        console.log('CAPTCHA is a canvas element');
        // Save canvas as image
        const base64Data = captchaText.replace(/^data:image\/png;base64,/, '');
        fs.writeFileSync(path.join(SCREENSHOTS_DIR, '07-captcha-canvas.png'), base64Data, 'base64');
        console.log(`CAPTCHA CANVAS: ${path.join(SCREENSHOTS_DIR, '07-captcha-canvas.png')}`);
      }
    } else {
      console.log('No CAPTCHA visible yet');
    }

    // Find and click submit/next
    console.log('\n=== STEP 7: Submit registration ===');
    const submitSelectors = [
      'button[type="submit"]',
      'button:has-text("Register")',
      'button:has-text("Submit")',
      'button:has-text("Create")',
      'button:has-text("Next")',
      'button:has-text("Continue")',
    ];

    for (const sel of submitSelectors) {
      try {
        const el = await page.$(sel);
        if (el) {
          const text = await el.textContent();
          console.log(`Found submit button: "${text}" (${sel})`);
          await el.click();
          await sleep(3000);
          console.log('URL after submit:', page.url());
          await screenshot(page, '08-after-submit');
          break;
        }
      } catch(e) {}
    }

    // Check for success or next steps
    console.log('\n=== STEP 8: Check post-submission state ===');
    const pageText = await page.evaluate(() => document.body.innerText.substring(0, 1000));
    console.log('Page text after submit:', pageText);
    await screenshot(page, '09-post-submit-state');

    // If on login page, log in
    const currentUrl = page.url();
    console.log('Current URL:', currentUrl);

    if (currentUrl.includes('login') || await page.$('input[type="email"]')) {
      console.log('\n=== STEP 9: Login with studentD credentials ===');

      // Fill email
      const emailEl = await page.$('input[type="email"]') || await page.$('input[name="email"]');
      if (emailEl) {
        await emailEl.fill('studentD@school.com');
        console.log('Filled login email');
      }

      // Fill password
      const passEl = await page.$('input[type="password"]');
      if (passEl) {
        await passEl.fill('Test@12345');
        console.log('Filled login password');
      }

      await screenshot(page, '10-login-filled');

      // Check for CAPTCHA on login page
      const loginCaptcha = await page.$('canvas, .captcha, [class*="captcha"]');
      if (loginCaptcha) {
        console.log('CAPTCHA on login page!');
        await screenshot(page, '10b-login-captcha');
      }

      // Submit login
      const loginBtn = await page.$('button[type="submit"]') || await page.$('button:has-text("Login")') || await page.$('button:has-text("Sign In")');
      if (loginBtn) {
        await loginBtn.click();
        await sleep(3000);
        console.log('URL after login:', page.url());
        await screenshot(page, '11-after-login');
      }
    }

    // Final state
    await sleep(2000);
    console.log('\n=== FINAL STATE ===');
    console.log('Final URL:', page.url());
    const finalText = await page.evaluate(() => document.body.innerText.substring(0, 500));
    console.log('Final page text:', finalText);
    await screenshot(page, '12-final-dashboard');

  } catch (err) {
    console.error('ERROR:', err.message);
    await screenshot(page, 'error-state');
  } finally {
    await browser.close();
    console.log('\n=== All screenshots saved to:', SCREENSHOTS_DIR, '===');
  }
}

main();
