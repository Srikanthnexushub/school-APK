-- Add optimistic locking version columns to support @Version fields in JPA entities.
-- Attendance is intentionally immutable (delete-and-recreate pattern) and does not need a version column.
ALTER TABLE center_schema.centers ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE center_schema.teachers ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
