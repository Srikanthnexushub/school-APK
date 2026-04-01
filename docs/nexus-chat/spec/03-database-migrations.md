# NexusChat — Database Migrations (Full SQL)

## Location: `services/nexus-chat-svc/src/main/resources/db/migration/chat/`

---

## V1__create_chat_schema.sql

```sql
CREATE SCHEMA IF NOT EXISTS chat_schema;
```

---

## V2__create_chat_sessions.sql

```sql
CREATE TABLE chat_schema.chat_sessions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    user_role       VARCHAR(30) NOT NULL,
    page_context    VARCHAR(100),
    title           VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    message_count   INTEGER     NOT NULL DEFAULT 0,
    last_active_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_chat_sessions_user
    ON chat_schema.chat_sessions(user_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_chat_sessions_user_active
    ON chat_schema.chat_sessions(user_id, last_active_at DESC)
    WHERE deleted_at IS NULL AND status = 'ACTIVE';
```

---

## V3__create_chat_messages.sql

```sql
CREATE TABLE chat_schema.chat_messages (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID        NOT NULL
                    REFERENCES chat_schema.chat_sessions(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL
                    CHECK (role IN ('user', 'assistant', 'system')),
    content         TEXT        NOT NULL,
    message_type    VARCHAR(30) NOT NULL DEFAULT 'TEXT'
                    CHECK (message_type IN ('TEXT', 'ACTION_RESULT', 'CONTEXT_CARD')),
    action_payload  JSONB,
    input_tokens    INTEGER,
    output_tokens   INTEGER,
    latency_ms      INTEGER,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Append-only: BRIN index for time-range queries
CREATE INDEX idx_chat_messages_session_time
    ON chat_schema.chat_messages(session_id, created_at ASC);

CREATE INDEX idx_chat_messages_created_brin
    ON chat_schema.chat_messages USING BRIN (created_at);
```

---

## V4__create_context_snapshots.sql

```sql
CREATE TABLE chat_schema.context_snapshots (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID        NOT NULL
                    REFERENCES chat_schema.chat_sessions(id) ON DELETE CASCADE,
    snapshot_json   JSONB       NOT NULL,
    captured_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_context_snapshots_session
    ON chat_schema.context_snapshots(session_id, captured_at DESC);

CREATE INDEX idx_context_snapshots_expires
    ON chat_schema.context_snapshots(expires_at);
```

---

## V5__create_proactive_nudges.sql

```sql
CREATE TABLE chat_schema.proactive_nudges (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    trigger_type    VARCHAR(50) NOT NULL
                    CHECK (trigger_type IN (
                        'EXAM_SUBMITTED',
                        'WEAK_AREA_CRITICAL',
                        'STUDY_PLAN_CREATED',
                        'INACTIVITY',
                        'EXAM_APPROACHING'
                    )),
    trigger_payload JSONB       NOT NULL,
    message         TEXT        NOT NULL,
    action_url      VARCHAR(255),
    delivered       BOOLEAN     NOT NULL DEFAULT false,
    opened          BOOLEAN     NOT NULL DEFAULT false,
    delivered_at    TIMESTAMPTZ,
    opened_at       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_proactive_nudges_user_undelivered
    ON chat_schema.proactive_nudges(user_id, created_at DESC)
    WHERE delivered = false;

CREATE INDEX idx_proactive_nudges_user_unopened
    ON chat_schema.proactive_nudges(user_id, created_at DESC)
    WHERE opened = false;
```

---

## PostgreSQL DB Setup (Manual — first time only on local dev)

```sql
-- Run as superuser (srikanth)
CREATE DATABASE nexus_chat_db;
CREATE USER nexus_chat_user WITH PASSWORD 'nexus_chat_pass_dev';
GRANT ALL PRIVILEGES ON DATABASE nexus_chat_db TO nexus_chat_user;
\c nexus_chat_db
GRANT CREATE ON SCHEMA public TO nexus_chat_user;
-- Flyway will create chat_schema automatically via V1 migration
```

## .env DB entries to add

```bash
NEXUS_CHAT_DB_NAME=nexus_chat_db
NEXUS_CHAT_DB_USER=nexus_chat_user
NEXUS_CHAT_DB_PASSWORD=nexus_chat_pass_dev
NEXUS_CHAT_DB_POOL_MAX_SIZE=5
NEXUS_CHAT_DB_POOL_MIN_IDLE=2
NEXUS_CHAT_DB_CONNECTION_TIMEOUT_MS=30000
NEXUS_CHAT_DB_IDLE_TIMEOUT_MS=600000
```
