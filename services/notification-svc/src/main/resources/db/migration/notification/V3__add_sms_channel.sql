-- V3: add recipient_phone column to support SMS channel delivery
-- Also widens the channel column constraint to allow the SMS value.

ALTER TABLE notification_schema.notifications
    ADD COLUMN recipient_phone VARCHAR(20);

-- Index for SMS delivery audit queries (look up all SMS to a given number)
CREATE INDEX idx_notifications_sms_phone ON notification_schema.notifications(recipient_phone)
    WHERE channel = 'SMS';
