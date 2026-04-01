# NexusChat — Config, .env, Gateway & Root POM Changes

## 1. .env additions

Add these lines to the project root `.env` file:

```bash
# ─── nexus-chat-svc ──────────────────────────────────────────
NEXUS_CHAT_SVC_PORT=8097
NEXUS_CHAT_DB_NAME=nexus_chat_db
NEXUS_CHAT_DB_USER=nexus_chat_user
NEXUS_CHAT_DB_PASSWORD=nexus_chat_pass_dev
NEXUS_CHAT_DB_POOL_MAX_SIZE=5
NEXUS_CHAT_DB_POOL_MIN_IDLE=2
NEXUS_CHAT_DB_CONNECTION_TIMEOUT_MS=30000
NEXUS_CHAT_DB_IDLE_TIMEOUT_MS=600000
NEXUS_CHAT_EVENTS_TOPIC=nexus-chat-events
NEXUS_CHAT_SVC_URI=http://localhost:8097
```

---

## 2. api-gateway/src/main/resources/application.yml — add route

Find the existing routes list and add the nexus-chat route **before** any catch-all routes:

```yaml
# ADD this route block inside spring.cloud.gateway.routes list:
- id: nexus-chat-svc
  uri: ${NEXUS_CHAT_SVC_URI:http://localhost:8097}
  predicates:
    - Path=/api/v1/chat/**
  filters:
    - RewritePath=/api/v1/chat/(?<segment>.*), /api/v1/chat/${segment}
```

**Placement**: Add it after the `ai-gateway-svc` route and before the student-gateway fallback.

---

## 3. student-gateway/src/main/resources/application.yml — add route (dual-gateway rule)

Per Fix #127 (dual-gateway rule): student-facing routes must be in BOTH gateways.
Students will use the chat widget → add same route to student-gateway:

```yaml
# ADD this route block inside spring.cloud.gateway.routes list:
- id: nexus-chat-svc
  uri: ${NEXUS_CHAT_SVC_URI:http://localhost:8097}
  predicates:
    - Path=/api/v1/chat/**
  filters:
    - RewritePath=/api/v1/chat/(?<segment>.*), /api/v1/chat/${segment}
```

---

## 4. Root pom.xml — add module

Find the `<modules>` section in the root `pom.xml` and add:

```xml
<module>services/nexus-chat-svc</module>
```

Add it after `<module>services/ai-gateway-svc</module>`.

---

## 5. PostgreSQL DB Setup (one-time local)

Run as superuser (`psql -U srikanth`):

```sql
CREATE DATABASE nexus_chat_db;
CREATE USER nexus_chat_user WITH PASSWORD 'nexus_chat_pass_dev';
GRANT ALL PRIVILEGES ON DATABASE nexus_chat_db TO nexus_chat_user;
\c nexus_chat_db
GRANT CREATE ON SCHEMA public TO nexus_chat_user;
```

---

## 6. start-all.sh — add nexus-chat-svc entry

In `scripts/start-all.sh`, add nexus-chat-svc to the service list with port 8097.
Pattern matches existing entries (e.g., ai-gateway-svc):

```bash
start_svc "nexus-chat-svc" "8097" "war"
```

---

## 7. AppLayout.tsx — exact changes needed

File: `frontend/web/src/layouts/AppLayout.tsx`

**Change 1** — Add imports (at top with other imports):
```tsx
import NexusChatWidget from '../components/chat/NexusChatWidget';
import { useNudgePoller } from '../hooks/useNudgePoller';
```

**Change 2** — Add hook call (inside AppLayout component body, after existing hooks):
```tsx
useNudgePoller();
```

**Change 3** — Add widget (just before the final `</div>` of the component return):
```tsx
<NexusChatWidget />
```

These are the **only** changes to AppLayout.tsx — no restructuring, no layout changes.

---

## 8. Summary of files touched (non-new)

| File | Change | Risk |
|---|---|---|
| `.env` | Append 10 lines | None — new keys only |
| `api-gateway/application.yml` | Add 1 route block | Low — new path, no overlap |
| `student-gateway/application.yml` | Add 1 route block | Low — same |
| `pom.xml` (root) | Add 1 `<module>` line | None |
| `scripts/start-all.sh` | Add 1 `start_svc` call | None |
| `AppLayout.tsx` | Add 2 imports + 2 lines | Low — additive only |
