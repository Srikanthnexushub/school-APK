-- V13__add_staff_profile_fields.sql
-- Adds role-based staff profile fields to the teachers table.
-- All columns are nullable to preserve backward compatibility with existing rows.

ALTER TABLE center_schema.teachers
    ADD COLUMN IF NOT EXISTS role_type           VARCHAR(30),
    ADD COLUMN IF NOT EXISTS qualification       VARCHAR(500),
    ADD COLUMN IF NOT EXISTS years_of_experience INTEGER,
    ADD COLUMN IF NOT EXISTS designation         VARCHAR(200),
    ADD COLUMN IF NOT EXISTS bio                 TEXT;

-- Update the status check constraint to include all valid enum values
-- (ACTIVE, INACTIVE were the original two; PENDING_APPROVAL + INVITATION_SENT were
--  added implicitly in V10 — this migration makes the constraint explicit)
ALTER TABLE center_schema.teachers
    DROP CONSTRAINT IF EXISTS chk_teachers_status;

ALTER TABLE center_schema.teachers
    ADD CONSTRAINT chk_teachers_status
        CHECK (status IN ('ACTIVE','INACTIVE','PENDING_APPROVAL','INVITATION_SENT'));

-- Index for efficient staff listing by role type within a center
CREATE INDEX IF NOT EXISTS idx_teachers_center_role_type
    ON center_schema.teachers (center_id, role_type)
    WHERE deleted_at IS NULL;
