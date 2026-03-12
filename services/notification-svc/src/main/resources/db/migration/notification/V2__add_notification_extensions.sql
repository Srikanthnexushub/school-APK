-- Extend notifications table with in-app delivery tracking fields
ALTER TABLE notification_schema.notifications
    ADD COLUMN notification_type VARCHAR(50),
    ADD COLUMN read_at            TIMESTAMPTZ,
    ADD COLUMN action_url         VARCHAR(500);

-- Partial index: fast lookup of unread IN_APP notifications per recipient
CREATE INDEX idx_notifications_inapp_unread
    ON notification_schema.notifications(recipient_id, created_at DESC)
    WHERE channel = 'IN_APP' AND read_at IS NULL;

-- Composite index for in-app inbox queries
CREATE INDEX idx_notifications_inapp_recipient
    ON notification_schema.notifications(recipient_id, channel, created_at DESC);
