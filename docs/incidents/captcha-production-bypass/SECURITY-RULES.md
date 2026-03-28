# CAPTCHA Security Rules — Strict Enforcement

> These rules are NON-NEGOTIABLE. No exception, no workaround, no override.
> Any modification requires written sign-off from the lead engineer.
> See INCIDENT-REPORT.md for why these rules exist.

---

## Rule 1 — CAPTCHA_E2E_BYPASS_TOKEN must be EMPTY in production

**What:** `/opt/apps/.env.prod` on EC2 must have `CAPTCHA_E2E_BYPASS_TOKEN=` (empty value).

**Why:** Setting it to any non-empty string disables real CAPTCHA verification for any
user who knows the token, enabling brute-force login attacks.

**Enforcement (code-level):** `LocalCaptchaVerifierAdapter.validateBypassTokenNotSetInProduction()`
is a `@PostConstruct` method that throws `IllegalStateException` at startup if
`APP_ENVIRONMENT` contains "prod" AND the bypass token is non-empty. auth-svc will
**refuse to start** — you cannot override this without code changes.

**Check command:**
```bash
ssh ec2-user@13.203.158.45 "grep CAPTCHA_E2E_BYPASS_TOKEN /opt/apps/.env.prod"
# Expected: CAPTCHA_E2E_BYPASS_TOKEN=
```

---

## Rule 2 — Cache-bust parameter is permanent in CaptchaWidget

**What:** `api.get('/api/v1/captcha/challenge?t=${Date.now()}')` in `CaptchaWidget.tsx`.

**Why:** Without the timestamp, browsers cache the JSON response. If a bypass response
was ever cached (as happened in this incident), users would see a green rectangle
forever until they manually cleared browser cache.

**You cannot "clean up" this parameter** — it is a security control, not dead code.

---

## Rule 3 — Restart BOTH services after any .env.prod change

**What:** After any change to `/opt/apps/.env.prod`, restart:
```bash
sudo systemctl restart edutech-auth-svc
sudo systemctl restart edutech-api-gateway
```

**Why:** Systemd `EnvironmentFile` is loaded once at process start. The running
process keeps the old env vars in memory. Only a restart picks up the new values.

---

## Rule 4 — Verify after every deployment

After any captcha-related change (auth-svc WAR deploy, .env.prod change, frontend deploy):

```bash
# 1. Challenge endpoint returns real UUID and large image
curl http://localhost/api/v1/captcha/challenge | python3 -c \
  "import sys,json; d=json.load(sys.stdin); \
   assert len(d['id'])==36, 'Not a UUID!'; \
   assert len(d.get('imageDataUri',''))>10000, 'Image too small — bypass active?'; \
   print('OK: id='+d['id'][:8]+'... imageLen='+str(len(d['imageDataUri'])))"

# 2. auth-svc health
curl -s http://localhost:8182/actuator/health | python3 -c "import sys,json; d=json.load(sys.stdin); assert d['status']=='UP'"
echo "auth-svc: OK"
```

---

## Rule 5 — Local .env != production .env.prod

| File | CAPTCHA_E2E_BYPASS_TOKEN | Purpose |
|------|--------------------------|---------|
| `.env` (project root, local dev) | `E2E-LOCAL-BYPASS-DO-NOT-USE-IN-PROD` | Local E2E tests only |
| `/opt/apps/.env.prod` (EC2) | `` (empty) | Production — NEVER set |

**Never copy `.env` to EC2.** Never paste the local bypass token into `.env.prod`.

---

## Automatic Enforcement Summary

| Layer | Mechanism | What Happens if Violated |
|-------|-----------|--------------------------|
| **Java startup** | `@PostConstruct` in `LocalCaptchaVerifierAdapter` | auth-svc **refuses to start** with `IllegalStateException` |
| **Frontend** | `?t=${Date.now()}` on every challenge fetch | Browser cache never serves stale response |
| **Documentation** | This file + INCIDENT-REPORT.md + frozen-fixes.md Fix #134 | Permanent record — referenced in CLAUDE.md |
| **Memory** | `memory/frozen-fixes.md` Fix #134 | Claude Code AI assistant enforces in all sessions |
