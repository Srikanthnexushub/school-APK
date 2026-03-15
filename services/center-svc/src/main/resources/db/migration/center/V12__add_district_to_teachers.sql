-- V12: add district to teachers for teacher self-registration address capture
ALTER TABLE center_schema.teachers
    ADD COLUMN IF NOT EXISTS district VARCHAR(100);
