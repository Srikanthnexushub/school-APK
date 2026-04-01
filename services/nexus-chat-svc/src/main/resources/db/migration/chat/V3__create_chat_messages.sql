CREATE TABLE chat_schema.chat_messages (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID        NOT NULL
                    REFERENCES chat_schema.chat_sessions(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL
                    CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    content         TEXT        NOT NULL,
    message_type    VARCHAR(30) NOT NULL DEFAULT 'TEXT'
                    CHECK (message_type IN ('TEXT', 'ACTION_RESULT', 'CONTEXT_CARD')),
    action_payload  JSONB,
    input_tokens    INTEGER,
    output_tokens   INTEGER,
    latency_ms      INTEGER,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_messages_session_time
    ON chat_schema.chat_messages(session_id, created_at ASC);

CREATE INDEX idx_chat_messages_created_brin
    ON chat_schema.chat_messages USING BRIN (created_at);
