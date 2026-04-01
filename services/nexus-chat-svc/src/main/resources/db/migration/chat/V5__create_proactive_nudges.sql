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
