-- V31__add_mode_to_batches.sql
ALTER TABLE center_schema.batches
    ADD COLUMN mode VARCHAR(20) NOT NULL DEFAULT 'OFFLINE';

ALTER TABLE center_schema.batches
    ADD CONSTRAINT chk_batches_mode CHECK (mode IN ('ONLINE', 'OFFLINE'));
