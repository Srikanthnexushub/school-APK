-- V4__announcement_read_receipts.sql
-- Tracks which recipients have read a given announcement.
-- Keyed on (announcement_id, recipient_id) — announcement_id is a UUID reference
-- to center_schema.announcements (cross-schema reference, enforced at app layer).
CREATE TABLE notification_schema.announcement_read_receipts (
    announcement_id UUID        NOT NULL,
    recipient_id    UUID        NOT NULL,
    read_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (announcement_id, recipient_id)
);
CREATE INDEX idx_ann_receipts_ann ON notification_schema.announcement_read_receipts(announcement_id);
