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
