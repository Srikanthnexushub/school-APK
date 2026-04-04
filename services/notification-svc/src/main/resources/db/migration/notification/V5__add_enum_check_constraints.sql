-- V5: Add CHECK constraints for notification channel and status enums
-- Channel values verified from NotificationChannel.java: EMAIL, PUSH, IN_APP, SMS
-- Status values verified from NotificationStatus.java: PENDING, SENT, FAILED

ALTER TABLE notification_schema.notifications
    DROP CONSTRAINT IF EXISTS notifications_channel_check;

ALTER TABLE notification_schema.notifications
    DROP CONSTRAINT IF EXISTS notifications_status_check;

ALTER TABLE notification_schema.notifications
    ADD CONSTRAINT notifications_channel_check
    CHECK (channel IN ('EMAIL', 'PUSH', 'IN_APP', 'SMS'));

ALTER TABLE notification_schema.notifications
    ADD CONSTRAINT notifications_status_check
    CHECK (status IN ('PENDING', 'SENT', 'FAILED'));
