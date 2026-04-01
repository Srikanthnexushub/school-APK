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
