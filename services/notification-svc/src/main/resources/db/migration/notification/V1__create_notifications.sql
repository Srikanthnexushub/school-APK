CREATE SCHEMA IF NOT EXISTS notification_schema;

CREATE TABLE notification_schema.notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id    UUID NOT NULL,
    recipient_email VARCHAR(255),
    channel         VARCHAR(20) NOT NULL,
    subject         VARCHAR(500),
    body            TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count     INTEGER NOT NULL DEFAULT 0,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_notifications_recipient_id ON notification_schema.notifications(recipient_id);
CREATE INDEX idx_notifications_status ON notification_schema.notifications(status);
CREATE INDEX idx_notifications_created_at ON notification_schema.notifications(created_at DESC);
