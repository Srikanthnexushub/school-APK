-- V5__create_notification_preferences.sql
-- Per-parent, per-channel, per-event-type notification preferences.

CREATE TABLE parent_schema.notification_preferences (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id   UUID        NOT NULL REFERENCES parent_schema.parent_profiles(id),
    channel     TEXT        NOT NULL,
    event_type  TEXT        NOT NULL,
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_channel CHECK (channel IN ('EMAIL', 'SMS', 'PUSH')),
    CONSTRAINT chk_event_type CHECK (event_type IN (
        'FEE_DUE', 'BATCH_UPDATE', 'ATTENDANCE_ALERT', 'CONTENT_UPLOAD', 'WEEKLY_REPORT'
    ))
);

-- Each parent can have exactly one preference per channel+eventType combination
CREATE UNIQUE INDEX uq_notification_preferences
    ON parent_schema.notification_preferences(parent_id, channel, event_type);

CREATE INDEX idx_notification_preferences_parent_id
    ON parent_schema.notification_preferences(parent_id);

CREATE TRIGGER trg_notification_preferences_updated_at
    BEFORE UPDATE ON parent_schema.notification_preferences
    FOR EACH ROW EXECUTE FUNCTION parent_schema.set_updated_at();
