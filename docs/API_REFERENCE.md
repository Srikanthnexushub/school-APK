# EduTech AI Platform — API Reference

> **Base URL:** `https://api.edutech.com` (via api-gateway)
> **Authentication:** `Authorization: Bearer <access_token>` on all endpoints except where noted
> **Content-Type:** `application/json`
> **Error format:** RFC 7807 ProblemDetail

---

## Table of Contents

1. [Common Conventions](#common-conventions)
2. [Authentication — auth-svc](#authentication--auth-svc)
   - [POST /api/v1/auth/register](#post-apiv1authregister)
   - [POST /api/v1/auth/login](#post-apiv1authlogin)
   - [POST /api/v1/auth/refresh](#post-apiv1authrefresh)
   - [POST /api/v1/auth/logout](#post-apiv1authlogout)
   - [POST /api/v1/auth/logout/all](#post-apiv1authlogoutall)
   - [GET /api/v1/auth/me](#get-apiv1authme)
   - [POST /api/v1/otp/send](#post-apiv1otpsend)
   - [POST /api/v1/otp/verify](#post-apiv1otpverify)
   - [POST /api/v1/auth/biometrics](#post-apiv1authbiometrics)
3. [Coaching Centers — center-svc](#coaching-centers--center-svc)
   - [POST /api/v1/centers](#post-apiv1centers)
   - [GET /api/v1/centers](#get-apiv1centers)
   - [GET /api/v1/centers/{centerId}](#get-apiv1centerscenterid)
   - [PUT /api/v1/centers/{centerId}](#put-apiv1centerscenterid)
   - [POST /api/v1/centers/{centerId}/batches](#post-apiv1centerscenteridbatches)
   - [GET /api/v1/centers/{centerId}/batches](#get-apiv1centerscenteridbatches)
   - [GET /api/v1/centers/{centerId}/batches/{batchId}](#get-apiv1centerscenteridbatchesbatchid)
   - [PUT /api/v1/centers/{centerId}/batches/{batchId}](#put-apiv1centerscenteridbatchesbatchid)
   - [POST /api/v1/centers/{centerId}/teachers](#post-apiv1centerscenteridteachers)
   - [GET /api/v1/centers/{centerId}/teachers](#get-apiv1centerscenteridteachers)
   - [POST /api/v1/centers/{centerId}/batches/{batchId}/schedules](#post-apiv1centerscenteridbatchesbatchidschedules)
   - [GET /api/v1/centers/{centerId}/batches/{batchId}/schedules](#get-apiv1centerscenteridbatchesbatchidschedules)
   - [POST /api/v1/centers/{centerId}/fees](#post-apiv1centerscenteridfees)
   - [GET /api/v1/centers/{centerId}/fees](#get-apiv1centerscenteridfees)
   - [POST /api/v1/centers/{centerId}/batches/{batchId}/attendance](#post-apiv1centerscenteridbatchesbatchidattendance)
   - [GET /api/v1/centers/{centerId}/batches/{batchId}/attendance](#get-apiv1centerscenteridbatchesbatchidattendance)
   - [POST /api/v1/centers/{centerId}/content](#post-apiv1centerscenteridcontent)
   - [GET /api/v1/centers/{centerId}/content](#get-apiv1centerscenteridcontent)
4. [Parent Portal — parent-svc](#parent-portal--parent-svc)
   - [POST /api/v1/parents](#post-apiv1parents)
   - [GET /api/v1/parents/me](#get-apiv1parentsme)
   - [GET /api/v1/parents/{profileId}](#get-apiv1parentsprofileid)
   - [PUT /api/v1/parents/{profileId}](#put-apiv1parentsprofileid)
   - [POST /api/v1/parents/{profileId}/students](#post-apiv1parentsprofileidstudents)
   - [GET /api/v1/parents/{profileId}/students](#get-apiv1parentsprofileidstudents)
   - [DELETE /api/v1/parents/{profileId}/students/{linkId}](#delete-apiv1parentsprofileidstudentslinkid)
   - [POST /api/v1/parents/{profileId}/payments](#post-apiv1parentsprofileidpayments)
   - [GET /api/v1/parents/{profileId}/payments](#get-apiv1parentsprofileidpayments)
   - [POST /api/v1/parents/{profileId}/notification-preferences](#post-apiv1parentsprofileidnotification-preferences)
   - [GET /api/v1/parents/{profileId}/notification-preferences](#get-apiv1parentsprofileidnotification-preferences)
   - [PUT /api/v1/parents/{profileId}/notification-preferences/{prefId}](#put-apiv1parentsprofileidnotification-preferencesprefid)
5. [Assessments — assess-svc](#assessments--assess-svc)
   - [POST /api/v1/exams](#post-apiv1exams)
   - [GET /api/v1/exams/{examId}](#get-apiv1examsexamid)
   - [GET /api/v1/exams](#get-apiv1exams)
   - [PUT /api/v1/exams/{examId}/publish](#put-apiv1examsexamidpublish)
   - [POST /api/v1/exams/{examId}/questions](#post-apiv1examsexamidquestions)
   - [GET /api/v1/exams/{examId}/questions](#get-apiv1examsexamidquestions)
   - [POST /api/v1/exams/{examId}/enrollments](#post-apiv1examsexamidenrollments)
   - [GET /api/v1/exams/{examId}/enrollments](#get-apiv1examsexamidenrollments)
   - [POST /api/v1/exams/{examId}/submissions](#post-apiv1examsexamidsubmissions)
   - [POST /api/v1/exams/{examId}/submissions/{submissionId}/answers](#post-apiv1examsexamidsubmissionssubmissionidanswers)
   - [GET /api/v1/exams/{examId}/submissions/{submissionId}](#get-apiv1examsexamidsubmissionssubmissionid)
   - [GET /api/v1/exams/{examId}/grades](#get-apiv1examsexamidgrades)
   - [GET /api/v1/students/{studentId}/grades](#get-apiv1studentsstudentidgrades)
6. [Psychological Profiling — psych-svc](#psychological-profiling--psych-svc)
   - [POST /api/v1/psych/profiles](#post-apiv1psychprofiles)
   - [GET /api/v1/psych/profiles/{profileId}](#get-apiv1psychprofilesprofileid)
   - [GET /api/v1/psych/profiles](#get-apiv1psychprofiles)
   - [POST /api/v1/psych/profiles/{profileId}/sessions](#post-apiv1psychprofilesprofileidsessions)
   - [POST /api/v1/psych/profiles/{profileId}/sessions/{sessionId}/complete](#post-apiv1psychprofilesprofileidsessionssessionidcomplete)
   - [GET /api/v1/psych/profiles/{profileId}/sessions](#get-apiv1psychprofilesprofileidsessions)
   - [POST /api/v1/psych/profiles/{profileId}/career-mappings](#post-apiv1psychprofilesprofileidcareer-mappings)
   - [GET /api/v1/psych/profiles/{profileId}/career-mappings](#get-apiv1psychprofilesprofileidcareer-mappings)
7. [AI Gateway — ai-gateway-svc](#ai-gateway--ai-gateway-svc)
   - [POST /api/v1/ai/completions](#post-apiv1aicompletions)
   - [POST /api/v1/ai/embeddings](#post-apiv1aiembeddings)
   - [POST /api/v1/ai/career-predictions](#post-apiv1aicareer-predictions)

---

## Common Conventions

### Role Hierarchy

```
SUPER_ADMIN > CENTER_ADMIN > TEACHER > PARENT > STUDENT
```

Higher-privilege roles can perform all actions permitted to lower roles within their scope.

### Common Response Codes

| Code | Meaning |
|------|---------|
| 200  | OK |
| 201  | Created |
| 202  | Accepted (async operation initiated) |
| 204  | No Content |
| 400  | Validation error |
| 401  | Missing or invalid JWT |
| 403  | Insufficient role |
| 404  | Resource not found |
| 409  | Conflict (duplicate resource) |
| 422  | Business rule violation |
| 429  | Rate limit exceeded |
| 502  | AI provider error (ai-gateway-svc only) |

### Error Body (RFC 7807 ProblemDetail)

All services return errors in the following format:

```json
{
  "type": "https://edutech.com/problems/not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Exam with id 550e8400-e29b-41d4-a716-446655440000 not found"
}
```

### Shared Object: DeviceFingerprint

Used in register, login, and refresh requests to bind sessions to specific devices.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `userAgent` | string | Yes | Browser or client user-agent string |
| `deviceId` | string | No | Client-generated stable device identifier |
| `ipSubnet` | string | No | IP subnet hint (e.g., `192.168.1.0/24`) |

---

## Authentication — auth-svc

Routes through api-gateway: `/api/v1/auth/**`, `/api/v1/otp/**`

---

### POST /api/v1/auth/register

Register a new user account. Returns a token pair immediately on success; the account requires email verification before full access is granted.

**Auth required:** No

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `email` | string | Yes | Valid email format | User's email address |
| `password` | string | Yes | 8–128 characters | Account password |
| `role` | string (enum) | Yes | `SUPER_ADMIN`, `CENTER_ADMIN`, `TEACHER`, `PARENT`, `STUDENT` | Role to assign |
| `centerId` | UUID | No | — | Center to associate (required for non-SUPER_ADMIN roles) |
| `firstName` | string | Yes | Max 100 chars | User's first name |
| `lastName` | string | Yes | Max 100 chars | User's last name |
| `phoneNumber` | string | No | Max 20 chars | Contact phone number |
| `captchaToken` | string | Yes | — | CAPTCHA verification token |
| `deviceFingerprint` | DeviceFingerprint | Yes | See shared object | Device binding for refresh token |

**Response:** `201 Created`

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
  "accessTokenExpiresAt": "2026-03-07T16:00:00Z",
  "refreshTokenExpiresAt": "2026-03-14T12:00:00Z",
  "tokenType": "Bearer"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Validation failure (invalid email, short password, etc.) |
| 409 | Email already registered |
| 422 | CAPTCHA verification failed |
| 429 | Too many registration attempts |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "priya.sharma@example.com",
    "password": "Secure@1234",
    "role": "TEACHER",
    "centerId": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "Priya",
    "lastName": "Sharma",
    "phoneNumber": "+919876543210",
    "captchaToken": "03AGdBq25...",
    "deviceFingerprint": {
      "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
      "deviceId": "device-abc-123",
      "ipSubnet": "203.0.113.0/24"
    }
  }'
```

---

### POST /api/v1/auth/login

Authenticate an existing user and receive a JWT access/refresh token pair.

**Auth required:** No

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `email` | string | Yes | Registered email address |
| `password` | string | Yes | Account password |
| `captchaToken` | string | Yes | CAPTCHA verification token |
| `deviceFingerprint` | DeviceFingerprint | Yes | Device binding for refresh token |

**Response:** `200 OK`

```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
  "accessTokenExpiresAt": "2026-03-07T16:00:00Z",
  "refreshTokenExpiresAt": "2026-03-14T12:00:00Z",
  "tokenType": "Bearer"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing required fields |
| 401 | Invalid credentials |
| 422 | Account inactive or unverified |
| 429 | Rate limit exceeded (brute-force protection) |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "priya.sharma@example.com",
    "password": "Secure@1234",
    "captchaToken": "03AGdBq25...",
    "deviceFingerprint": {
      "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
      "deviceId": "device-abc-123",
      "ipSubnet": "203.0.113.0/24"
    }
  }'
```

---

### POST /api/v1/auth/refresh

Rotate the refresh token. Single-use and device-bound; the old refresh token is immediately invalidated.

**Auth required:** No

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `refreshToken` | string | Yes | The current valid refresh token |
| `deviceFingerprint` | DeviceFingerprint | Yes | Must match the fingerprint used when the token was issued |

**Response:** `200 OK` — same `TokenPair` structure as login.

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing fields |
| 401 | Refresh token invalid, expired, or already used |
| 403 | Device fingerprint mismatch |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
    "deviceFingerprint": {
      "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
      "deviceId": "device-abc-123",
      "ipSubnet": "203.0.113.0/24"
    }
  }'
```

---

### POST /api/v1/auth/logout

Revoke the current session's refresh token.

**Auth required:** Yes (any role)

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `refreshToken` | string | Yes | The refresh token to revoke |
| `deviceFingerprint` | DeviceFingerprint | Yes | Device fingerprint for validation |

**Response:** `204 No Content`

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 401 | Missing or invalid access token |
| 404 | Refresh token not found |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
    "deviceFingerprint": {
      "userAgent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)",
      "deviceId": "device-abc-123"
    }
  }'
```

---

### POST /api/v1/auth/logout/all

Revoke all active refresh tokens for the authenticated user across all devices.

**Auth required:** Yes (any role)

**Request Body:** None

**Response:** `204 No Content`

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 401 | Missing or invalid access token |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/auth/logout/all \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### GET /api/v1/auth/me

Get the profile of the currently authenticated user.

**Auth required:** Yes (any role)

**Response:** `200 OK`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "priya.sharma@example.com",
  "firstName": "Priya",
  "lastName": "Sharma",
  "phoneNumber": "+919876543210",
  "role": "TEACHER",
  "status": "ACTIVE",
  "centerId": "661f9511-f3ac-52e5-b827-557766551111",
  "createdAt": "2026-01-15T10:30:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 401 | Missing or invalid access token |

**Example:**

```bash
curl https://api.edutech.com/api/v1/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### POST /api/v1/otp/send

Send a one-time password to the specified email address via the chosen channel.

**Auth required:** No

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `email` | string | Yes | Valid email | Recipient email address |
| `purpose` | string | Yes | — | OTP purpose (e.g., `EMAIL_VERIFICATION`, `PASSWORD_RESET`) |
| `channel` | string | Yes | `email` or `sms` | Delivery channel |

**Response:** `202 Accepted` — No body.

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Invalid email or channel value |
| 404 | Email not associated with any account |
| 429 | Too many OTP requests |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/otp/send \
  -H "Content-Type: application/json" \
  -d '{
    "email": "priya.sharma@example.com",
    "purpose": "EMAIL_VERIFICATION",
    "channel": "email"
  }'
```

---

### POST /api/v1/otp/verify

Verify a one-time password. If the purpose is `EMAIL_VERIFICATION`, the account is activated upon success.

**Auth required:** No

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `email` | string | Yes | Valid email | Email for which OTP was sent |
| `otp` | string | Yes | Exactly 6 characters | The one-time password received |
| `purpose` | string | Yes | — | Must match the purpose used when sending |

**Response:** `204 No Content`

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing fields or OTP not exactly 6 characters |
| 401 | OTP incorrect or expired |
| 404 | No pending OTP found for this email and purpose |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/otp/verify \
  -H "Content-Type: application/json" \
  -d '{
    "email": "priya.sharma@example.com",
    "otp": "482019",
    "purpose": "EMAIL_VERIFICATION"
  }'
```

---

### POST /api/v1/auth/biometrics

Analyze keystroke dynamics from a login session and return a behavioral risk score. Scores range from 0.0 (low risk, consistent human typing) to 1.0 (high risk, bot-like behavior).

**Auth required:** Yes (any role)

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `keystrokes` | array of KeystrokeEvent | Yes | Ordered list of key press events captured during typing |
| `sessionId` | string | Yes | Session identifier correlating keystroke data |

**KeystrokeEvent object:**

| Field | Type | Description |
|-------|------|-------------|
| `keyDownTime` | long | Epoch milliseconds when the key was pressed |
| `keyUpTime` | long | Epoch milliseconds when the key was released |
| `key` | string | The key identifier (e.g., `"a"`, `"Shift"`) |

**Response:** `200 OK`

```json
{
  "score": 0.14,
  "level": "LOW",
  "sessionId": "sess-7f3a2b1c-4d5e-6f7a-8b9c-0d1e2f3a4b5c"
}
```

`level` is one of: `LOW` (score < 0.3), `MEDIUM` (0.3–0.7), `HIGH` (> 0.7).

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Empty keystroke list or missing sessionId |
| 401 | Missing or invalid access token |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/auth/biometrics \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "sess-7f3a2b1c-4d5e-6f7a-8b9c-0d1e2f3a4b5c",
    "keystrokes": [
      { "keyDownTime": 1709808000000, "keyUpTime": 1709808000085, "key": "p" },
      { "keyDownTime": 1709808000140, "keyUpTime": 1709808000215, "key": "r" },
      { "keyDownTime": 1709808000270, "keyUpTime": 1709808000340, "key": "i" }
    ]
  }'
```

---

## Coaching Centers — center-svc

Routes through api-gateway: `/api/v1/centers/**`

All endpoints require `Authorization: Bearer <access_token>`.

---

### POST /api/v1/centers

Create a new coaching center. Only SUPER_ADMIN may perform this action.

**Auth required:** Yes — `SUPER_ADMIN`

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | Yes | Max 200 chars | Center display name |
| `code` | string | Yes | Max 20 chars, uppercase alphanumeric (`^[A-Z0-9]+$`) | Unique short code |
| `address` | string | Yes | Max 500 chars | Street address |
| `city` | string | Yes | Max 100 chars | City |
| `state` | string | Yes | Max 100 chars | State |
| `pincode` | string | Yes | Max 10 chars | Postal code |
| `phone` | string | Yes | Max 20 chars | Contact phone number |
| `email` | string | Yes | Valid email, max 255 chars | Contact email |
| `website` | string | No | Max 500 chars | Center website URL |
| `logoUrl` | string | No | Max 1000 chars | URL of the center's logo |
| `ownerId` | UUID | No | — | User ID to set as center owner |

**Response:** `201 Created`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Bright Futures Academy",
  "code": "BFA001",
  "address": "12 MG Road",
  "city": "Bengaluru",
  "state": "Karnataka",
  "pincode": "560001",
  "phone": "+918012345678",
  "email": "admin@brightfutures.edu",
  "website": "https://brightfutures.edu",
  "logoUrl": "https://cdn.edutech.com/logos/bfa001.png",
  "status": "ACTIVE",
  "ownerId": "661f9511-f3ac-52e5-b827-557766551111",
  "createdAt": "2026-03-07T10:00:00Z",
  "updatedAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Validation failure |
| 403 | Caller is not SUPER_ADMIN |
| 409 | Center code already exists |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/centers \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bright Futures Academy",
    "code": "BFA001",
    "address": "12 MG Road",
    "city": "Bengaluru",
    "state": "Karnataka",
    "pincode": "560001",
    "phone": "+918012345678",
    "email": "admin@brightfutures.edu",
    "website": "https://brightfutures.edu",
    "ownerId": "661f9511-f3ac-52e5-b827-557766551111"
  }'
```

---

### GET /api/v1/centers

List coaching centers accessible to the authenticated caller. SUPER_ADMIN sees all centers; CENTER_ADMIN and below see only their own center.

**Auth required:** Yes (any role)

**Response:** `200 OK` — Array of CenterResponse objects (see POST response schema).

**Example:**

```bash
curl https://api.edutech.com/api/v1/centers \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### GET /api/v1/centers/{centerId}

Get full details of a specific coaching center.

**Auth required:** Yes (any role with center access)

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |

**Response:** `200 OK` — CenterResponse object (see POST response schema).

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 403 | Caller does not have access to this center |
| 404 | Center not found |

**Example:**

```bash
curl https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### PUT /api/v1/centers/{centerId}

Update mutable details of a coaching center.

**Auth required:** Yes — `SUPER_ADMIN` or `CENTER_ADMIN` of this center

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | Yes | Max 200 chars | Center display name |
| `address` | string | Yes | Max 500 chars | Street address |
| `city` | string | Yes | Max 100 chars | City |
| `state` | string | Yes | Max 100 chars | State |
| `pincode` | string | Yes | Max 10 chars | Postal code |
| `phone` | string | Yes | Max 20 chars | Contact phone number |
| `email` | string | Yes | Valid email, max 255 chars | Contact email |
| `website` | string | No | Max 500 chars | Center website URL |
| `logoUrl` | string | No | Max 1000 chars | URL of center logo |

**Response:** `200 OK` — Updated CenterResponse object.

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Validation failure |
| 403 | Insufficient role |
| 404 | Center not found |

**Example:**

```bash
curl -X PUT https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bright Futures Academy — Koramangala",
    "address": "45 Sarjapur Road",
    "city": "Bengaluru",
    "state": "Karnataka",
    "pincode": "560034",
    "phone": "+918012345678",
    "email": "koramangala@brightfutures.edu"
  }'
```

---

### POST /api/v1/centers/{centerId}/batches

Create a new batch within a center.

**Auth required:** Yes — `CENTER_ADMIN` or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | Yes | Max 200 chars | Batch name |
| `code` | string | Yes | Max 50 chars | Unique batch code within the center |
| `subject` | string | Yes | Max 100 chars | Subject or course name |
| `teacherId` | UUID | No | — | Assigned teacher's user ID |
| `maxStudents` | int | Yes | 1–200 | Maximum student capacity |
| `startDate` | date | Yes | ISO-8601 (`YYYY-MM-DD`) | Batch start date |
| `endDate` | date | No | ISO-8601 (`YYYY-MM-DD`) | Batch end date |

**Response:** `201 Created`

```json
{
  "id": "772g0622-g4bd-63f6-c938-668877662222",
  "centerId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "JEE Advanced 2027 — Batch A",
  "code": "JEE-A-2027",
  "subject": "Physics, Chemistry, Mathematics",
  "teacherId": "661f9511-f3ac-52e5-b827-557766551111",
  "maxStudents": 40,
  "enrolledCount": 0,
  "startDate": "2026-06-01",
  "endDate": "2027-04-30",
  "status": "ACTIVE",
  "createdAt": "2026-03-07T10:00:00Z",
  "updatedAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Validation failure |
| 403 | Insufficient role |
| 404 | Center not found |
| 409 | Batch code already exists in this center |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/batches \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "JEE Advanced 2027 — Batch A",
    "code": "JEE-A-2027",
    "subject": "Physics, Chemistry, Mathematics",
    "teacherId": "661f9511-f3ac-52e5-b827-557766551111",
    "maxStudents": 40,
    "startDate": "2026-06-01",
    "endDate": "2027-04-30"
  }'
```

---

### GET /api/v1/centers/{centerId}/batches

List batches for a center, optionally filtered by status.

**Auth required:** Yes (any role with center access)

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |

**Query Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `status` | string (BatchStatus enum) | No | Filter by status (e.g., `ACTIVE`, `COMPLETED`, `CANCELLED`) |

**Response:** `200 OK` — Array of BatchResponse objects.

**Example:**

```bash
curl "https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/batches?status=ACTIVE" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### GET /api/v1/centers/{centerId}/batches/{batchId}

Get details of a specific batch.

**Auth required:** Yes (any role with center access)

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |
| `batchId` | UUID | Batch identifier |

**Response:** `200 OK` — BatchResponse object (see POST /batches response schema).

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 404 | Center or batch not found |
| 403 | Insufficient access |

**Example:**

```bash
curl https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/batches/772g0622-g4bd-63f6-c938-668877662222 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### PUT /api/v1/centers/{centerId}/batches/{batchId}

Update a batch's status or teacher assignment.

**Auth required:** Yes — `CENTER_ADMIN` or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |
| `batchId` | UUID | Batch identifier |

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `teacherId` | UUID | No | New teacher assignment (null removes the assignment) |
| `status` | string (BatchStatus) | Yes | Updated batch status |

**Response:** `200 OK` — Updated BatchResponse object.

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing required status field |
| 403 | Insufficient role |
| 404 | Batch not found |

**Example:**

```bash
curl -X PUT https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/batches/772g0622-g4bd-63f6-c938-668877662222 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "teacherId": "883h1733-h5ce-74g7-d049-779988773333",
    "status": "ACTIVE"
  }'
```

---

### POST /api/v1/centers/{centerId}/teachers

Assign a user as a teacher to the specified center.

**Auth required:** Yes — `CENTER_ADMIN` or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `userId` | UUID | Yes | — | Auth-svc user ID to assign as teacher |
| `firstName` | string | Yes | Max 100 chars | Teacher's first name |
| `lastName` | string | Yes | Max 100 chars | Teacher's last name |
| `email` | string | Yes | Valid email, max 255 chars | Teacher's email |
| `phoneNumber` | string | No | Max 20 chars | Teacher's phone number |
| `subjects` | string | No | Max 500 chars | Comma-separated list of subjects taught |

**Response:** `201 Created`

```json
{
  "id": "994i2844-i6df-85h8-e150-880099884444",
  "centerId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "661f9511-f3ac-52e5-b827-557766551111",
  "firstName": "Priya",
  "lastName": "Sharma",
  "email": "priya.sharma@example.com",
  "phoneNumber": "+919876543210",
  "subjects": "Physics, Mathematics",
  "status": "ACTIVE",
  "joinedAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Validation failure |
| 403 | Insufficient role |
| 404 | Center or user not found |
| 409 | User already assigned to this center |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/teachers \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "661f9511-f3ac-52e5-b827-557766551111",
    "firstName": "Priya",
    "lastName": "Sharma",
    "email": "priya.sharma@example.com",
    "phoneNumber": "+919876543210",
    "subjects": "Physics, Mathematics"
  }'
```

---

### GET /api/v1/centers/{centerId}/teachers

List all teachers assigned to a center.

**Auth required:** Yes (any role with center access)

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |

**Response:** `200 OK` — Array of TeacherResponse objects (see POST /teachers response schema).

**Example:**

```bash
curl https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/teachers \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### POST /api/v1/centers/{centerId}/batches/{batchId}/schedules

Add a recurring schedule slot to a batch.

**Auth required:** Yes — `CENTER_ADMIN`, `TEACHER`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |
| `batchId` | UUID | Batch identifier |

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `dayOfWeek` | string | Yes | Day of week (`MONDAY`, `TUESDAY`, …, `SUNDAY`) |
| `startTime` | string | Yes | ISO-8601 local time (`HH:mm:ss`) |
| `endTime` | string | Yes | ISO-8601 local time (`HH:mm:ss`) |
| `room` | string | Yes | Max 100 chars — Room or venue name |
| `effectiveFrom` | date | Yes | ISO-8601 date (`YYYY-MM-DD`) — Schedule start date |
| `effectiveTo` | date | No | ISO-8601 date (`YYYY-MM-DD`) — Schedule end date |

**Response:** `201 Created`

```json
{
  "id": "aa5j3955-j7eg-96i9-f261-991100995555",
  "batchId": "772g0622-g4bd-63f6-c938-668877662222",
  "centerId": "550e8400-e29b-41d4-a716-446655440000",
  "dayOfWeek": "MONDAY",
  "startTime": "09:00:00",
  "endTime": "11:00:00",
  "room": "Lab 3",
  "effectiveFrom": "2026-06-01",
  "effectiveTo": "2027-04-30",
  "createdAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Invalid time format or missing fields |
| 403 | Insufficient role |
| 404 | Batch or center not found |
| 409 | Room conflict for the same time slot |

**Example:**

```bash
curl -X POST "https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/batches/772g0622-g4bd-63f6-c938-668877662222/schedules" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "dayOfWeek": "MONDAY",
    "startTime": "09:00:00",
    "endTime": "11:00:00",
    "room": "Lab 3",
    "effectiveFrom": "2026-06-01",
    "effectiveTo": "2027-04-30"
  }'
```

---

### GET /api/v1/centers/{centerId}/batches/{batchId}/schedules

List all schedule slots for a batch.

**Auth required:** Yes (any role with center access)

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |
| `batchId` | UUID | Batch identifier |

**Response:** `200 OK` — Array of ScheduleResponse objects.

**Example:**

```bash
curl "https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/batches/772g0622-g4bd-63f6-c938-668877662222/schedules" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### POST /api/v1/centers/{centerId}/fees

Create a fee structure for a center.

**Auth required:** Yes — `CENTER_ADMIN` or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | Yes | Max 200 chars | Fee structure name |
| `description` | string | No | Max 1000 chars | Description of what is included |
| `amount` | decimal | Yes | Min 0.01 | Fee amount |
| `currency` | string | Yes | Max 5 chars (e.g., `INR`, `USD`) | Currency code |
| `frequency` | string (FeeFrequency enum) | Yes | — | Billing frequency (e.g., `MONTHLY`, `QUARTERLY`, `ANNUALLY`) |
| `dueDay` | int | Yes | 1–31 | Day of month when payment is due |
| `lateFeeAmount` | decimal | No | Min 0.00 | Late payment penalty amount |

**Response:** `201 Created`

```json
{
  "id": "bb6k4066-k8fh-07j0-g372-002211006666",
  "centerId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "JEE Standard Monthly Plan",
  "description": "Monthly fee for JEE Advanced batch",
  "amount": 5000.00,
  "currency": "INR",
  "frequency": "MONTHLY",
  "dueDay": 5,
  "lateFeeAmount": 250.00,
  "status": "ACTIVE",
  "createdAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Validation failure |
| 403 | Insufficient role |
| 404 | Center not found |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/fees \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "JEE Standard Monthly Plan",
    "description": "Monthly fee for JEE Advanced batch",
    "amount": 5000.00,
    "currency": "INR",
    "frequency": "MONTHLY",
    "dueDay": 5,
    "lateFeeAmount": 250.00
  }'
```

---

### GET /api/v1/centers/{centerId}/fees

List active fee structures for a center.

**Auth required:** Yes (any role with center access)

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |

**Response:** `200 OK` — Array of FeeStructureResponse objects.

**Example:**

```bash
curl https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/fees \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### POST /api/v1/centers/{centerId}/batches/{batchId}/attendance

Mark or re-mark attendance for an entire batch on a specific date. Re-posting for the same date replaces existing records.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |
| `batchId` | UUID | Batch identifier |

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `date` | date | Yes | ISO-8601 date (`YYYY-MM-DD`) — Attendance date |
| `entries` | array of AttendanceEntry | Yes | One entry per student |

**AttendanceEntry object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `studentId` | UUID | Yes | Student's user ID |
| `status` | string (AttendanceStatus enum) | Yes | `PRESENT`, `ABSENT`, `LATE`, or `EXCUSED` |
| `notes` | string | No | Optional note for this student |

**Response:** `201 Created` — Array of AttendanceResponse objects.

```json
[
  {
    "id": "cc7l5177-l9gi-18k1-h483-113322117777",
    "batchId": "772g0622-g4bd-63f6-c938-668877662222",
    "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
    "date": "2026-03-07",
    "status": "PRESENT",
    "markedByTeacherId": "994i2844-i6df-85h8-e150-880099884444",
    "notes": null,
    "createdAt": "2026-03-07T09:05:00Z"
  }
]
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Invalid date format or empty entries list |
| 403 | Insufficient role |
| 404 | Batch or student not found |

**Example:**

```bash
curl -X POST "https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/batches/772g0622-g4bd-63f6-c938-668877662222/attendance" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2026-03-07",
    "entries": [
      { "studentId": "dd8m6288-m0hj-29l2-i594-224433228888", "status": "PRESENT" },
      { "studentId": "ee9n7399-n1ik-30m3-j605-335544339999", "status": "ABSENT", "notes": "Called in sick" }
    ]
  }'
```

---

### GET /api/v1/centers/{centerId}/batches/{batchId}/attendance

Retrieve attendance records for a batch on a specific date.

**Auth required:** Yes (any role with center access)

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |
| `batchId` | UUID | Batch identifier |

**Query Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `date` | date | Yes | ISO-8601 date (`YYYY-MM-DD`) — Date to query |

**Response:** `200 OK` — Array of AttendanceResponse objects.

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing or invalid date format |
| 404 | Batch not found |

**Example:**

```bash
curl "https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/batches/772g0622-g4bd-63f6-c938-668877662222/attendance?date=2026-03-07" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### POST /api/v1/centers/{centerId}/content

Register a content item's metadata. The file must already be uploaded to CDN or S3 before calling this endpoint.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `batchId` | UUID | No | — | Batch this content belongs to (null = center-wide) |
| `title` | string | Yes | Max 500 chars | Content title |
| `description` | string | No | Max 2000 chars | Content description |
| `type` | string (ContentType enum) | Yes | — | Type (e.g., `PDF`, `VIDEO`, `AUDIO`, `IMAGE`, `DOCUMENT`) |
| `fileUrl` | string | Yes | Max 2000 chars | CDN/S3 URL of the uploaded file |
| `fileSizeBytes` | long | No | Min 0 | File size in bytes |

**Response:** `201 Created`

```json
{
  "id": "ff0o8400-o2jl-41n4-k716-446655440000",
  "centerId": "550e8400-e29b-41d4-a716-446655440000",
  "batchId": "772g0622-g4bd-63f6-c938-668877662222",
  "title": "Chapter 3 — Kinematics Notes",
  "description": "Detailed handwritten notes on projectile motion",
  "type": "PDF",
  "fileUrl": "https://cdn.edutech.com/content/ch3-kinematics.pdf",
  "fileSizeBytes": 2048000,
  "uploadedByUserId": "994i2844-i6df-85h8-e150-880099884444",
  "status": "ACTIVE",
  "createdAt": "2026-03-07T11:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Validation failure |
| 403 | Insufficient role |
| 404 | Center not found |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/content \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "batchId": "772g0622-g4bd-63f6-c938-668877662222",
    "title": "Chapter 3 — Kinematics Notes",
    "description": "Detailed handwritten notes on projectile motion",
    "type": "PDF",
    "fileUrl": "https://cdn.edutech.com/content/ch3-kinematics.pdf",
    "fileSizeBytes": 2048000
  }'
```

---

### GET /api/v1/centers/{centerId}/content

List all content items for a center.

**Auth required:** Yes (any role with center access)

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `centerId` | UUID | Center identifier |

**Response:** `200 OK` — Array of ContentItemResponse objects.

**Example:**

```bash
curl https://api.edutech.com/api/v1/centers/550e8400-e29b-41d4-a716-446655440000/content \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

## Parent Portal — parent-svc

Routes through api-gateway: `/api/v1/parents/**`

All endpoints require `Authorization: Bearer <access_token>`.

---

### POST /api/v1/parents

Create a parent profile for the authenticated user. A user may only have one parent profile.

**Auth required:** Yes — `PARENT` role

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `name` | string | Yes | Non-blank | Parent's display name |
| `phone` | string | No | Pattern `^\+?[0-9]{7,15}$` | Contact phone number |

**Response:** `201 Created`

```json
{
  "id": "gg1p9511-p3km-52o5-l827-557766551111",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Ramesh Kumar",
  "phone": "+919123456789",
  "verified": false,
  "status": "ACTIVE",
  "createdAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Invalid phone number format |
| 403 | Caller does not have PARENT role |
| 409 | Profile already exists for this user |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/parents \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ramesh Kumar",
    "phone": "+919123456789"
  }'
```

---

### GET /api/v1/parents/me

Get the authenticated parent's own profile.

**Auth required:** Yes — `PARENT` role

**Response:** `200 OK` — ParentProfileResponse (see POST /parents response schema).

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 404 | Profile not yet created for this user |

**Example:**

```bash
curl https://api.edutech.com/api/v1/parents/me \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### GET /api/v1/parents/{profileId}

Get a parent profile by its ID. Accessible by SUPER_ADMIN and CENTER_ADMIN; parents may only access their own profile.

**Auth required:** Yes — `PARENT` (own profile only), `CENTER_ADMIN`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Parent profile identifier |

**Response:** `200 OK` — ParentProfileResponse.

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 403 | Caller attempting to access another parent's profile |
| 404 | Profile not found |

**Example:**

```bash
curl https://api.edutech.com/api/v1/parents/gg1p9511-p3km-52o5-l827-557766551111 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### PUT /api/v1/parents/{profileId}

Update a parent profile's name and/or phone number.

**Auth required:** Yes — profile owner or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Parent profile identifier |

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | No | Updated display name |
| `phone` | string | No | Updated phone number |

**Response:** `200 OK` — Updated ParentProfileResponse.

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 403 | Not the profile owner |
| 404 | Profile not found |

**Example:**

```bash
curl -X PUT https://api.edutech.com/api/v1/parents/gg1p9511-p3km-52o5-l827-557766551111 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{ "name": "Ramesh Kumar Pillai", "phone": "+919900001234" }'
```

---

### POST /api/v1/parents/{profileId}/students

Link a student to this parent profile.

**Auth required:** Yes — profile owner or `CENTER_ADMIN`/`SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Parent profile identifier |

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `studentId` | UUID | Yes | Auth-svc user ID of the student |
| `studentName` | string | Yes | Student's display name |
| `centerId` | UUID | Yes | Center where the student is enrolled |

**Response:** `201 Created`

```json
{
  "id": "hh2q0622-q4ln-63p6-m938-668877662222",
  "parentId": "gg1p9511-p3km-52o5-l827-557766551111",
  "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
  "studentName": "Ananya Kumar",
  "centerId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ACTIVE",
  "createdAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing required fields |
| 403 | Insufficient access |
| 404 | Parent profile or student not found |
| 409 | Student already linked to this parent |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/parents/gg1p9511-p3km-52o5-l827-557766551111/students \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
    "studentName": "Ananya Kumar",
    "centerId": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

---

### GET /api/v1/parents/{profileId}/students

List all active student links for a parent profile.

**Auth required:** Yes — profile owner or `CENTER_ADMIN`/`SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Parent profile identifier |

**Response:** `200 OK` — Array of StudentLinkResponse objects.

**Example:**

```bash
curl https://api.edutech.com/api/v1/parents/gg1p9511-p3km-52o5-l827-557766551111/students \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### DELETE /api/v1/parents/{profileId}/students/{linkId}

Revoke a student link from a parent profile.

**Auth required:** Yes — profile owner or `CENTER_ADMIN`/`SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Parent profile identifier |
| `linkId` | UUID | Student link identifier |

**Response:** `204 No Content`

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 403 | Not the profile owner |
| 404 | Link not found |

**Example:**

```bash
curl -X DELETE https://api.edutech.com/api/v1/parents/gg1p9511-p3km-52o5-l827-557766551111/students/hh2q0622-q4ln-63p6-m938-668877662222 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### POST /api/v1/parents/{profileId}/payments

Record a fee payment for a student.

**Auth required:** Yes — profile owner or `CENTER_ADMIN`/`SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Parent profile identifier |

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `studentId` | UUID | Yes | — | Student for whom payment is made |
| `centerId` | UUID | Yes | — | Center receiving the payment |
| `batchId` | UUID | No | — | Specific batch this payment is for |
| `amountPaid` | decimal | Yes | Min 0.01 | Amount paid |
| `currency` | string | No | — | Currency code (defaults to center's currency) |
| `paymentDate` | date | Yes | ISO-8601 (`YYYY-MM-DD`) | Date the payment was made |
| `referenceNumber` | string | Yes | Non-blank | Bank/gateway transaction reference |
| `remarks` | string | No | — | Optional notes |

**Response:** `201 Created`

```json
{
  "id": "ii3r1733-r5mo-74q7-n049-779988773333",
  "parentId": "gg1p9511-p3km-52o5-l827-557766551111",
  "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
  "centerId": "550e8400-e29b-41d4-a716-446655440000",
  "batchId": "772g0622-g4bd-63f6-c938-668877662222",
  "amountPaid": 5000.00,
  "currency": "INR",
  "paymentDate": "2026-03-05",
  "referenceNumber": "NEFT20260305XYZ123",
  "remarks": "March fee",
  "status": "RECORDED",
  "createdAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Validation failure |
| 403 | Insufficient access |
| 404 | Parent profile, student, or center not found |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/parents/gg1p9511-p3km-52o5-l827-557766551111/payments \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
    "centerId": "550e8400-e29b-41d4-a716-446655440000",
    "batchId": "772g0622-g4bd-63f6-c938-668877662222",
    "amountPaid": 5000.00,
    "currency": "INR",
    "paymentDate": "2026-03-05",
    "referenceNumber": "NEFT20260305XYZ123",
    "remarks": "March fee"
  }'
```

---

### GET /api/v1/parents/{profileId}/payments

List all fee payment records for a parent profile.

**Auth required:** Yes — profile owner or `CENTER_ADMIN`/`SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Parent profile identifier |

**Response:** `200 OK` — Array of FeePaymentResponse objects.

**Example:**

```bash
curl https://api.edutech.com/api/v1/parents/gg1p9511-p3km-52o5-l827-557766551111/payments \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### POST /api/v1/parents/{profileId}/notification-preferences

Create or update a notification preference by channel and event type (upsert semantics).

**Auth required:** Yes — profile owner or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Parent profile identifier |

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `channel` | string (NotificationChannel enum) | Yes | Delivery channel (e.g., `EMAIL`, `SMS`, `PUSH`) |
| `eventType` | string | Yes | Event type identifier (e.g., `FEE_DUE`, `ATTENDANCE_MARKED`, `EXAM_RESULT`) |
| `enabled` | boolean | Yes | Whether notifications for this channel+event are enabled |

**Response:** `200 OK`

```json
{
  "id": "jj4s2844-s6np-85r8-o150-880099884444",
  "parentId": "gg1p9511-p3km-52o5-l827-557766551111",
  "channel": "SMS",
  "eventType": "ATTENDANCE_MARKED",
  "enabled": true,
  "createdAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing required fields |
| 403 | Not the profile owner |
| 404 | Profile not found |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/parents/gg1p9511-p3km-52o5-l827-557766551111/notification-preferences \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "channel": "SMS",
    "eventType": "ATTENDANCE_MARKED",
    "enabled": true
  }'
```

---

### GET /api/v1/parents/{profileId}/notification-preferences

List all notification preferences for a parent profile.

**Auth required:** Yes — profile owner or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Parent profile identifier |

**Response:** `200 OK` — Array of NotificationPreferenceResponse objects.

**Example:**

```bash
curl https://api.edutech.com/api/v1/parents/gg1p9511-p3km-52o5-l827-557766551111/notification-preferences \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### PUT /api/v1/parents/{profileId}/notification-preferences/{prefId}

Update the enabled/disabled state of a specific notification preference.

**Auth required:** Yes — profile owner or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Parent profile identifier |
| `prefId` | UUID | Notification preference identifier |

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `enabled` | boolean | Yes | New enabled state |

**Response:** `200 OK` — Updated NotificationPreferenceResponse.

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing enabled field |
| 403 | Not the profile owner |
| 404 | Preference not found |

**Example:**

```bash
curl -X PUT https://api.edutech.com/api/v1/parents/gg1p9511-p3km-52o5-l827-557766551111/notification-preferences/jj4s2844-s6np-85r8-o150-880099884444 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{ "enabled": false }'
```

---

## Assessments — assess-svc

Routes through api-gateway: `/api/v1/exams/**`, `/api/v1/students/**`

All endpoints require `Authorization: Bearer <access_token>`.

---

### POST /api/v1/exams

Create a new exam for a batch.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `title` | string | Yes | Non-blank | Exam title |
| `description` | string | No | — | Exam description |
| `batchId` | UUID | Yes | — | Batch this exam belongs to |
| `centerId` | UUID | Yes | — | Center this exam belongs to |
| `mode` | string (ExamMode enum) | Yes | — | Mode: `ONLINE` or `OFFLINE` |
| `durationMinutes` | int | Yes | Min 1 | Exam duration in minutes |
| `maxAttempts` | int | Yes | Min 1 | Maximum allowed attempts per student |
| `startAt` | datetime | No | ISO-8601 instant | Exam window open time |
| `endAt` | datetime | No | ISO-8601 instant | Exam window close time |
| `totalMarks` | double | Yes | Min 0.01 | Total marks available |
| `passingMarks` | double | Yes | Min 0.01 | Marks required to pass |

**Response:** `201 Created`

```json
{
  "id": "kk5t3955-t7oq-96s9-p261-991100995555",
  "batchId": "772g0622-g4bd-63f6-c938-668877662222",
  "centerId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Physics Unit Test — Kinematics",
  "description": "Chapters 1-3 of NCERT Physics Part 1",
  "mode": "ONLINE",
  "durationMinutes": 90,
  "maxAttempts": 1,
  "startAt": "2026-03-10T09:00:00Z",
  "endAt": "2026-03-10T12:00:00Z",
  "totalMarks": 100.0,
  "passingMarks": 40.0,
  "status": "DRAFT",
  "createdAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Validation failure |
| 403 | Insufficient role |
| 404 | Batch or center not found |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/exams \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Physics Unit Test — Kinematics",
    "description": "Chapters 1-3 of NCERT Physics Part 1",
    "batchId": "772g0622-g4bd-63f6-c938-668877662222",
    "centerId": "550e8400-e29b-41d4-a716-446655440000",
    "mode": "ONLINE",
    "durationMinutes": 90,
    "maxAttempts": 1,
    "startAt": "2026-03-10T09:00:00Z",
    "endAt": "2026-03-10T12:00:00Z",
    "totalMarks": 100.0,
    "passingMarks": 40.0
  }'
```

---

### GET /api/v1/exams/{examId}

Get full details of a specific exam.

**Auth required:** Yes (any role with access to the exam's batch)

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `examId` | UUID | Exam identifier |

**Response:** `200 OK` — ExamResponse (see POST /exams response schema).

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 403 | No access to this exam's batch |
| 404 | Exam not found |

**Example:**

```bash
curl https://api.edutech.com/api/v1/exams/kk5t3955-t7oq-96s9-p261-991100995555 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### GET /api/v1/exams

List all exams for a given batch.

**Auth required:** Yes (any role with access to the batch)

**Query Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `batchId` | UUID | Yes | Filter exams by batch |

**Response:** `200 OK` — Array of ExamResponse objects.

**Example:**

```bash
curl "https://api.edutech.com/api/v1/exams?batchId=772g0622-g4bd-63f6-c938-668877662222" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### PUT /api/v1/exams/{examId}/publish

Publish an exam, making it visible and accessible to enrolled students. Only DRAFT exams can be published.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `examId` | UUID | Exam identifier |

**Request Body:** None

**Response:** `200 OK` — Updated ExamResponse with `status: "PUBLISHED"`.

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 403 | Insufficient role |
| 404 | Exam not found |
| 422 | Exam is not in DRAFT status, or has no questions |

**Example:**

```bash
curl -X PUT https://api.edutech.com/api/v1/exams/kk5t3955-t7oq-96s9-p261-991100995555/publish \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### POST /api/v1/exams/{examId}/questions

Add a multiple-choice question to an exam. The exam must be in DRAFT status.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `examId` | UUID | Exam identifier |

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `questionText` | string | Yes | Non-blank | The question text |
| `options` | array of string | Yes | Min 2 items | Answer options |
| `correctAnswer` | int | Yes | Min 0 | Zero-based index of the correct option |
| `explanation` | string | No | — | Explanation of the correct answer |
| `marks` | double | Yes | Min 0.01 | Marks awarded for a correct answer |
| `difficulty` | double | No | — | IRT difficulty parameter (b-parameter) |
| `discrimination` | double | No | — | IRT discrimination parameter (a-parameter) |
| `guessingParam` | double | No | — | IRT guessing parameter (c-parameter) |

**Response:** `201 Created`

```json
{
  "id": "ll6u4066-u8pr-07t0-q372-002211006666",
  "examId": "kk5t3955-t7oq-96s9-p261-991100995555",
  "questionText": "A ball is thrown horizontally at 20 m/s. What is the horizontal displacement after 3 seconds?",
  "options": ["40 m", "60 m", "80 m", "100 m"],
  "correctAnswer": 1,
  "explanation": "Horizontal displacement = v × t = 20 × 3 = 60 m",
  "marks": 2.0,
  "difficulty": 0.4,
  "discrimination": 1.2,
  "guessingParam": 0.25,
  "createdAt": "2026-03-07T10:30:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Fewer than 2 options, or correctAnswer out of range |
| 403 | Insufficient role |
| 404 | Exam not found |
| 422 | Exam is not in DRAFT status |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/exams/kk5t3955-t7oq-96s9-p261-991100995555/questions \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "questionText": "A ball is thrown horizontally at 20 m/s. What is the horizontal displacement after 3 seconds?",
    "options": ["40 m", "60 m", "80 m", "100 m"],
    "correctAnswer": 1,
    "explanation": "Horizontal displacement = v × t = 20 × 3 = 60 m",
    "marks": 2.0,
    "difficulty": 0.4,
    "discrimination": 1.2,
    "guessingParam": 0.25
  }'
```

---

### GET /api/v1/exams/{examId}/questions

List all questions for an exam.

**Auth required:** Yes (any role with access to the exam)

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `examId` | UUID | Exam identifier |

**Response:** `200 OK` — Array of QuestionResponse objects.

**Example:**

```bash
curl https://api.edutech.com/api/v1/exams/kk5t3955-t7oq-96s9-p261-991100995555/questions \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### POST /api/v1/exams/{examId}/enrollments

Enroll a student in an exam.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `examId` | UUID | Exam identifier |

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `studentId` | UUID | Yes | Auth-svc user ID of the student to enroll |

**Response:** `201 Created`

```json
{
  "id": "mm7v5177-v9qs-18u1-r483-113322117777",
  "examId": "kk5t3955-t7oq-96s9-p261-991100995555",
  "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
  "status": "ENROLLED",
  "enrolledAt": "2026-03-07T11:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing studentId |
| 403 | Insufficient role |
| 404 | Exam or student not found |
| 409 | Student already enrolled |
| 422 | Exam is not in PUBLISHED status |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/exams/kk5t3955-t7oq-96s9-p261-991100995555/enrollments \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{ "studentId": "dd8m6288-m0hj-29l2-i594-224433228888" }'
```

---

### GET /api/v1/exams/{examId}/enrollments

List all enrollments for an exam.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `examId` | UUID | Exam identifier |

**Response:** `200 OK` — Array of EnrollmentResponse objects.

**Example:**

```bash
curl https://api.edutech.com/api/v1/exams/kk5t3955-t7oq-96s9-p261-991100995555/enrollments \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### POST /api/v1/exams/{examId}/submissions

Start a new exam submission for the authenticated student.

**Auth required:** Yes — `STUDENT` role

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `examId` | UUID | Exam identifier |

**Request Body:** None

**Response:** `201 Created`

```json
{
  "id": "nn8w6288-w0rt-29v2-s594-224433228888",
  "examId": "kk5t3955-t7oq-96s9-p261-991100995555",
  "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
  "startedAt": "2026-03-10T09:05:00Z",
  "submittedAt": null,
  "totalMarks": 100.0,
  "scoredMarks": 0.0,
  "percentage": 0.0,
  "status": "IN_PROGRESS",
  "attemptNumber": 1
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 403 | Caller is not STUDENT or not enrolled |
| 404 | Exam not found |
| 422 | Exam not yet open, already closed, or max attempts exceeded |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/exams/kk5t3955-t7oq-96s9-p261-991100995555/submissions \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### POST /api/v1/exams/{examId}/submissions/{submissionId}/answers

Submit answers for an in-progress exam submission.

**Auth required:** Yes — `STUDENT` (submission owner)

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `examId` | UUID | Exam identifier |
| `submissionId` | UUID | Submission identifier |

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `answers` | array of AnswerEntry | Yes | One entry per answered question |

**AnswerEntry object:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `questionId` | UUID | Yes | Question identifier |
| `selectedOption` | int | Yes | Zero-based index of the chosen answer option |

**Response:** `200 OK` — Completed SubmissionResponse with scores calculated.

```json
{
  "id": "nn8w6288-w0rt-29v2-s594-224433228888",
  "examId": "kk5t3955-t7oq-96s9-p261-991100995555",
  "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
  "startedAt": "2026-03-10T09:05:00Z",
  "submittedAt": "2026-03-10T10:30:00Z",
  "totalMarks": 100.0,
  "scoredMarks": 72.0,
  "percentage": 72.0,
  "status": "SUBMITTED",
  "attemptNumber": 1
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Empty answers list |
| 403 | Not the submission owner |
| 404 | Submission or question not found |
| 422 | Submission is not in IN_PROGRESS status |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/exams/kk5t3955-t7oq-96s9-p261-991100995555/submissions/nn8w6288-w0rt-29v2-s594-224433228888/answers \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "answers": [
      { "questionId": "ll6u4066-u8pr-07t0-q372-002211006666", "selectedOption": 1 },
      { "questionId": "oo9x7399-x1su-30w3-t605-335544339999", "selectedOption": 0 }
    ]
  }'
```

---

### GET /api/v1/exams/{examId}/submissions/{submissionId}

Get the details and current state of a submission.

**Auth required:** Yes — submission owner, `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `examId` | UUID | Exam identifier |
| `submissionId` | UUID | Submission identifier |

**Response:** `200 OK` — SubmissionResponse.

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 403 | Not the submission owner or insufficient role |
| 404 | Submission not found |

**Example:**

```bash
curl https://api.edutech.com/api/v1/exams/kk5t3955-t7oq-96s9-p261-991100995555/submissions/nn8w6288-w0rt-29v2-s594-224433228888 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### GET /api/v1/exams/{examId}/grades

List grade records for all students who submitted the exam.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `examId` | UUID | Exam identifier |

**Response:** `200 OK`

```json
[
  {
    "id": "pp0y8400-y2tv-41x4-u716-446655440000",
    "submissionId": "nn8w6288-w0rt-29v2-s594-224433228888",
    "examId": "kk5t3955-t7oq-96s9-p261-991100995555",
    "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
    "batchId": "772g0622-g4bd-63f6-c938-668877662222",
    "centerId": "550e8400-e29b-41d4-a716-446655440000",
    "percentage": 72.0,
    "letterGrade": "B+",
    "passed": true,
    "createdAt": "2026-03-10T10:35:00Z"
  }
]
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 403 | Insufficient role |
| 404 | Exam not found |

**Example:**

```bash
curl https://api.edutech.com/api/v1/exams/kk5t3955-t7oq-96s9-p261-991100995555/grades \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### GET /api/v1/students/{studentId}/grades

List all grade records for a specific student across all exams.

**Auth required:** Yes — the student themselves, their linked parent, `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `studentId` | UUID | Student's user ID |

**Response:** `200 OK` — Array of GradeResponse objects.

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 403 | No access to this student's records |
| 404 | Student not found |

**Example:**

```bash
curl https://api.edutech.com/api/v1/students/dd8m6288-m0hj-29l2-i594-224433228888/grades \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

## Psychological Profiling — psych-svc

Routes through api-gateway: `/api/v1/psych/**`

All endpoints require `Authorization: Bearer <access_token>`.

---

### POST /api/v1/psych/profiles

Create a psychometric profile for a student.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `studentId` | UUID | Yes | Student's user ID |
| `centerId` | UUID | Yes | Center context |
| `batchId` | UUID | Yes | Batch context |

**Response:** `201 Created`

```json
{
  "id": "qq1z9511-z3uw-52y5-v827-557766551111",
  "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
  "centerId": "550e8400-e29b-41d4-a716-446655440000",
  "batchId": "772g0622-g4bd-63f6-c938-668877662222",
  "openness": 0.0,
  "conscientiousness": 0.0,
  "extraversion": 0.0,
  "agreeableness": 0.0,
  "neuroticism": 0.0,
  "riasecCode": null,
  "status": "PENDING",
  "createdAt": "2026-03-07T10:00:00Z",
  "updatedAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing required fields |
| 403 | Insufficient role |
| 404 | Student, center, or batch not found |
| 409 | Profile already exists for this student in this batch |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/psych/profiles \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
    "centerId": "550e8400-e29b-41d4-a716-446655440000",
    "batchId": "772g0622-g4bd-63f6-c938-668877662222"
  }'
```

---

### GET /api/v1/psych/profiles/{profileId}

Get a psychometric profile by ID.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, `SUPER_ADMIN`, or the student themselves

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Psychometric profile identifier |

**Response:** `200 OK` — PsychProfileResponse.

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 403 | No access to this profile |
| 404 | Profile not found |

**Example:**

```bash
curl https://api.edutech.com/api/v1/psych/profiles/qq1z9511-z3uw-52y5-v827-557766551111 \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### GET /api/v1/psych/profiles

List all psychometric profiles for a center.

**Auth required:** Yes — `CENTER_ADMIN` or `SUPER_ADMIN`

**Query Parameters:**

| Name | Type | Required | Description |
|------|------|----------|-------------|
| `centerId` | UUID | Yes | Filter profiles by center |

**Response:** `200 OK` — Array of PsychProfileResponse objects.

**Example:**

```bash
curl "https://api.edutech.com/api/v1/psych/profiles?centerId=550e8400-e29b-41d4-a716-446655440000" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### POST /api/v1/psych/profiles/{profileId}/sessions

Start a new psychometric assessment session for a student profile.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Psychometric profile identifier |

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sessionType` | string (SessionType enum) | Yes | Session type (e.g., `INITIAL`, `FOLLOW_UP`, `ANNUAL_REVIEW`) |
| `scheduledAt` | datetime | Yes | ISO-8601 instant — Planned session time |

**Response:** `201 Created`

```json
{
  "id": "rr2a0622-a4vx-63z6-w938-668877662222",
  "profileId": "qq1z9511-z3uw-52y5-v827-557766551111",
  "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
  "sessionType": "INITIAL",
  "status": "SCHEDULED",
  "scheduledAt": "2026-03-15T10:00:00Z",
  "startedAt": null,
  "completedAt": null,
  "notes": null,
  "createdAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing sessionType or scheduledAt |
| 403 | Insufficient role |
| 404 | Profile not found |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/psych/profiles/qq1z9511-z3uw-52y5-v827-557766551111/sessions \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "sessionType": "INITIAL",
    "scheduledAt": "2026-03-15T10:00:00Z"
  }'
```

---

### POST /api/v1/psych/profiles/{profileId}/sessions/{sessionId}/complete

Complete a psychometric session by recording the Big Five personality scores and optionally the RIASEC code. This updates the parent profile's trait scores.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Psychometric profile identifier |
| `sessionId` | UUID | Session identifier |

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `openness` | double | Yes | 0.0–1.0 | Openness to experience score |
| `conscientiousness` | double | Yes | 0.0–1.0 | Conscientiousness score |
| `extraversion` | double | Yes | 0.0–1.0 | Extraversion score |
| `agreeableness` | double | Yes | 0.0–1.0 | Agreeableness score |
| `neuroticism` | double | Yes | 0.0–1.0 | Neuroticism score |
| `riasecCode` | string | No | — | RIASEC career type code (e.g., `"RIA"`, `"SEC"`) |
| `notes` | string | No | — | Clinician's session notes |

**Response:** `200 OK` — Updated SessionResponse.

```json
{
  "id": "rr2a0622-a4vx-63z6-w938-668877662222",
  "profileId": "qq1z9511-z3uw-52y5-v827-557766551111",
  "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
  "sessionType": "INITIAL",
  "status": "COMPLETED",
  "scheduledAt": "2026-03-15T10:00:00Z",
  "startedAt": "2026-03-15T10:03:00Z",
  "completedAt": "2026-03-15T11:45:00Z",
  "notes": "Student shows strong artistic inclination.",
  "createdAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Trait scores outside 0.0–1.0 range |
| 403 | Insufficient role |
| 404 | Session or profile not found |
| 422 | Session is not in SCHEDULED or IN_PROGRESS status |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/psych/profiles/qq1z9511-z3uw-52y5-v827-557766551111/sessions/rr2a0622-a4vx-63z6-w938-668877662222/complete \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "openness": 0.82,
    "conscientiousness": 0.71,
    "extraversion": 0.45,
    "agreeableness": 0.68,
    "neuroticism": 0.33,
    "riasecCode": "AIR",
    "notes": "Student shows strong artistic inclination."
  }'
```

---

### GET /api/v1/psych/profiles/{profileId}/sessions

List all psychometric sessions for a profile.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, `SUPER_ADMIN`, or the student themselves

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Psychometric profile identifier |

**Response:** `200 OK` — Array of SessionResponse objects.

**Example:**

```bash
curl https://api.edutech.com/api/v1/psych/profiles/qq1z9511-z3uw-52y5-v827-557766551111/sessions \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### POST /api/v1/psych/profiles/{profileId}/career-mappings

Request an AI-generated career mapping for the student's psychometric profile. The profile must have at least one completed session with trait scores.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Psychometric profile identifier |

**Request Body:** None

**Response:** `201 Created`

```json
{
  "id": "ss3b1733-b5wy-74a7-x049-779988773333",
  "profileId": "qq1z9511-z3uw-52y5-v827-557766551111",
  "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
  "status": "PENDING",
  "requestedAt": "2026-03-07T10:00:00Z",
  "generatedAt": null,
  "topCareers": null,
  "reasoning": null,
  "modelVersion": null,
  "createdAt": "2026-03-07T10:00:00Z"
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 403 | Insufficient role |
| 404 | Profile not found |
| 422 | Profile has no completed sessions with trait data |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/psych/profiles/qq1z9511-z3uw-52y5-v827-557766551111/career-mappings \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

### GET /api/v1/psych/profiles/{profileId}/career-mappings

List all career mapping results for a psychometric profile.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, `SUPER_ADMIN`, or the student themselves

**Path Parameters:**

| Name | Type | Description |
|------|------|-------------|
| `profileId` | UUID | Psychometric profile identifier |

**Response:** `200 OK` — Array of CareerMappingResponse objects.

```json
[
  {
    "id": "ss3b1733-b5wy-74a7-x049-779988773333",
    "profileId": "qq1z9511-z3uw-52y5-v827-557766551111",
    "studentId": "dd8m6288-m0hj-29l2-i594-224433228888",
    "status": "COMPLETED",
    "requestedAt": "2026-03-07T10:00:00Z",
    "generatedAt": "2026-03-07T10:02:15Z",
    "topCareers": "Architect, Industrial Designer, Graphic Designer",
    "reasoning": "High openness and conscientiousness combined with AIR RIASEC suggest careers in creative technical fields.",
    "modelVersion": "claude-3-opus-20240229",
    "createdAt": "2026-03-07T10:00:00Z"
  }
]
```

**Example:**

```bash
curl https://api.edutech.com/api/v1/psych/profiles/qq1z9511-z3uw-52y5-v827-557766551111/career-mappings \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..."
```

---

## AI Gateway — ai-gateway-svc

Routes through api-gateway: `/api/v1/ai/**`

All endpoints require `Authorization: Bearer <access_token>`. Responses are reactive (non-blocking). An additional `502 Bad Gateway` error is returned when the upstream AI provider is unavailable.

---

### POST /api/v1/ai/completions

Route a chat completion request to the appropriate LLM provider. Provider selection is handled automatically by the gateway based on routing configuration.

**Auth required:** Yes (any role)

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `requesterId` | string | Yes | Identifier of the requesting entity (user ID or service name) |
| `systemPrompt` | string | No | System-level instruction for the LLM |
| `userMessage` | string | Yes | The user's message or prompt |
| `maxTokens` | int | Yes | Maximum tokens in the generated response |
| `temperature` | double | Yes | Sampling temperature (0.0 = deterministic, 1.0 = creative) |

**Response:** `200 OK`

```json
{
  "requestId": "req-550e8400-e29b-41d4-a716-446655440000",
  "content": "Kinematics is the branch of mechanics that describes the motion of objects without considering the forces that cause the motion...",
  "provider": "OPENAI",
  "modelUsed": "gpt-4o",
  "inputTokens": 142,
  "outputTokens": 387,
  "latencyMs": 1243
}
```

`provider` is one of the configured `LlmProvider` enum values (e.g., `OPENAI`, `ANTHROPIC`, `GOOGLE`).

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing required fields |
| 401 | Missing or invalid access token |
| 429 | Rate limit exceeded |
| 502 | Upstream AI provider returned an error or timed out |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/ai/completions \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "requesterId": "550e8400-e29b-41d4-a716-446655440000",
    "systemPrompt": "You are a JEE Physics tutor. Provide concise, accurate explanations.",
    "userMessage": "Explain the concept of projectile motion.",
    "maxTokens": 512,
    "temperature": 0.3
  }'
```

---

### POST /api/v1/ai/embeddings

Generate a vector embedding for the provided text using the configured embedding model.

**Auth required:** Yes (any role)

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `requesterId` | string | Yes | Identifier of the requesting entity |
| `text` | string | Yes | Text to embed |
| `dimensions` | int | Yes | Number of dimensions for the output embedding vector |

**Response:** `200 OK`

```json
{
  "requestId": "req-661f9511-f3ac-52e5-b827-557766551111",
  "embedding": [0.0234, -0.1823, 0.5671, ...],
  "modelUsed": "text-embedding-3-small",
  "latencyMs": 88
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing or empty text, invalid dimensions |
| 401 | Missing or invalid access token |
| 429 | Rate limit exceeded |
| 502 | Upstream embedding provider error |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/ai/embeddings \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "requesterId": "550e8400-e29b-41d4-a716-446655440000",
    "text": "Projectile motion occurs when an object is launched into the air subject only to gravitational acceleration.",
    "dimensions": 1536
  }'
```

---

### POST /api/v1/ai/career-predictions

Submit a student's Big Five personality scores and RIASEC code to an AI model for career prediction. Typically called internally by psych-svc after a career mapping is requested, but may also be called directly.

**Auth required:** Yes — `TEACHER`, `CENTER_ADMIN`, or `SUPER_ADMIN`

**Request Body:**

| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| `requesterId` | string | Yes | — | Identifier of the requesting entity |
| `profileId` | string | Yes | — | Psychometric profile ID for correlation |
| `openness` | double | Yes | 0.0–1.0 | Big Five openness score |
| `conscientiousness` | double | Yes | 0.0–1.0 | Big Five conscientiousness score |
| `extraversion` | double | Yes | 0.0–1.0 | Big Five extraversion score |
| `agreeableness` | double | Yes | 0.0–1.0 | Big Five agreeableness score |
| `neuroticism` | double | Yes | 0.0–1.0 | Big Five neuroticism score |
| `riasecCode` | string | No | — | RIASEC career interest code (e.g., `"AIR"`) |

**Response:** `200 OK`

```json
{
  "requestId": "req-772g0622-g4bd-63f6-c938-668877662222",
  "topCareers": [
    "Architect",
    "Industrial Designer",
    "Urban Planner",
    "Product Designer",
    "Landscape Architect"
  ],
  "reasoning": "The combination of high openness (0.82) and conscientiousness (0.71) with an AIR RIASEC profile strongly indicates a preference for structured creative work. Artistic and investigative interests further suggest technical design fields.",
  "modelVersion": "claude-3-opus-20240229",
  "latencyMs": 2187
}
```

**Error Responses:**

| Status | Condition |
|--------|-----------|
| 400 | Missing required fields or trait scores out of range |
| 401 | Missing or invalid access token |
| 403 | Insufficient role |
| 429 | Rate limit exceeded |
| 502 | Upstream AI provider error |

**Example:**

```bash
curl -X POST https://api.edutech.com/api/v1/ai/career-predictions \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "requesterId": "550e8400-e29b-41d4-a716-446655440000",
    "profileId": "qq1z9511-z3uw-52y5-v827-557766551111",
    "openness": 0.82,
    "conscientiousness": 0.71,
    "extraversion": 0.45,
    "agreeableness": 0.68,
    "neuroticism": 0.33,
    "riasecCode": "AIR"
  }'
```

---

*Document generated: 2026-03-07. All field names derived from the Java record source files.*
