# NexusChat — API Contracts

## Base URL
All routes proxied via api-gateway: `POST /api/v1/chat/**` → `nexus-chat-svc:8097`

All endpoints require `Authorization: Bearer <JWT>` header.

---

## 1. Start Session

**POST** `/api/v1/chat/sessions`

### Request
```json
{
  "pageContext": "dashboard"
}
```

### Response `201 Created`
```json
{
  "sessionId": "3fa8e2c1-4d12-4b99-a3b0-8f1e2d3c4a5b",
  "title": null,
  "greeting": "Hey Arjun! 👋 I'm Nexus, your AI study partner. I see you're on the dashboard — want to review your weak areas in Physics?",
  "createdAt": "2026-04-01T10:00:00Z"
}
```

---

## 2. Stream Message (SSE)

**POST** `/api/v1/chat/sessions/{sessionId}/stream`

Produces: `text/event-stream`

### Request Body
```json
{
  "message": "What are my weakest topics right now?",
  "pageContext": "performance"
}
```

### SSE Response Stream
```
data: {"choices":[{"delta":{"content":"Based "}}]}

data: {"choices":[{"delta":{"content":"on "}}]}

data: {"choices":[{"delta":{"content":"your recent performance, "}}]}

...

data: [DONE]
```

**Last line is always `data: [DONE]`** — signals frontend to stop the spinner and persist.

### Error Response (SSE)
```
data: {"error":"SESSION_NOT_FOUND"}

data: [DONE]
```

---

## 3. Send Message (Blocking — for action messages)

**POST** `/api/v1/chat/sessions/{sessionId}/messages`

Use this for action-triggered messages (e.g., after ActionCommand is executed and result needs to be recorded).

### Request
```json
{
  "message": "I just navigated to the performance page",
  "messageType": "ACTION_RESULT",
  "actionPayload": {
    "action": "NAVIGATE",
    "path": "/performance",
    "success": true
  }
}
```

### Response `200 OK`
```json
{
  "messageId": "7bc2d1e3-...",
  "role": "ASSISTANT",
  "content": "Great! I can see your performance data. Let's focus on Physics — your mastery is at 42%.",
  "messageType": "TEXT",
  "createdAt": "2026-04-01T10:01:00Z"
}
```

---

## 4. Get Sessions List

**GET** `/api/v1/chat/sessions`

### Response `200 OK`
```json
[
  {
    "sessionId": "3fa8e2c1-...",
    "title": "Weak areas in Physics",
    "pageContext": "performance",
    "messageCount": 12,
    "lastActiveAt": "2026-04-01T10:05:00Z",
    "status": "ACTIVE"
  }
]
```

---

## 5. Get Session Messages

**GET** `/api/v1/chat/sessions/{sessionId}/messages`

### Response `200 OK`
```json
[
  {
    "messageId": "...",
    "role": "USER",
    "content": "What are my weakest topics?",
    "messageType": "TEXT",
    "actionPayload": null,
    "createdAt": "2026-04-01T10:00:30Z"
  },
  {
    "messageId": "...",
    "role": "ASSISTANT",
    "content": "Your top 3 weak areas are:\n1. **Thermodynamics** (38% mastery)\n2. **Electrochemistry** (41% mastery)\n3. **Organic Reactions** (44% mastery)\n\n[{\"action\":\"SHOW_WEAK_AREAS\",\"params\":{\"subjects\":[\"Physics\",\"Chemistry\"]}}]",
    "messageType": "TEXT",
    "actionPayload": null,
    "createdAt": "2026-04-01T10:00:35Z"
  }
]
```

---

## 6. Archive Session

**DELETE** `/api/v1/chat/sessions/{sessionId}`

### Response `204 No Content`

---

## 7. Get Proactive Nudges

**GET** `/api/v1/chat/nudges?limit=5`

### Response `200 OK`
```json
[
  {
    "nudgeId": "a1b2c3d4-...",
    "triggerType": "WEAK_AREA_CRITICAL",
    "message": "⚠️ Your Thermodynamics score just dropped to critical! Let's fix this together.",
    "actionUrl": "/chat?nudge=a1b2c3d4-...",
    "delivered": false,
    "opened": false,
    "createdAt": "2026-04-01T09:30:00Z"
  }
]
```

---

## 8. Mark Nudge Opened

**PATCH** `/api/v1/chat/nudges/{nudgeId}/opened`

### Response `200 OK`
```json
{ "nudgeId": "a1b2c3d4-...", "opened": true, "openedAt": "2026-04-01T10:10:00Z" }
```

---

## Error Codes

| HTTP | Code | Meaning |
|---|---|---|
| 404 | `SESSION_NOT_FOUND` | Session doesn't exist or belongs to another user |
| 403 | `SESSION_ACCESS_DENIED` | JWT userId ≠ session userId |
| 429 | `RATE_LIMIT_EXCEEDED` | >30 messages/hour per user |
| 500 | `CONTEXT_AGGREGATION_FAILED` | All 5 upstream calls failed (very rare) |
| 503 | `AI_GATEWAY_UNAVAILABLE` | ai-gateway-svc unreachable |

---

## Inter-Service API (nexus-chat-svc → ai-gateway-svc)

**POST** `http://ai-gateway-svc:8086/api/v1/ai/completions/stream`

Header: `X-Service-Key: ${AI_GATEWAY_SERVICE_KEY}`

### Request
```json
{
  "requesterId": "bc46d073-...",
  "systemPrompt": "You are Nexus, an AI study partner for NexusEd...",
  "history": [
    {"role": "user", "content": "What are my weakest topics?"},
    {"role": "assistant", "content": "Your top 3 weak areas are..."}
  ],
  "userMessage": "Can you create a study plan for Thermodynamics?",
  "maxTokens": 1200,
  "temperature": 0.7
}
```

### Response: SSE token stream (same `data: {"choices":[{"delta":{"content":"token"}}]}` format)
