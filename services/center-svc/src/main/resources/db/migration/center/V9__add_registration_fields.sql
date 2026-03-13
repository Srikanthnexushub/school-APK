-- V9__add_registration_fields.sql
-- Adds self-registration support: PENDING_VERIFICATION/REJECTED statuses,
-- nullable code (assigned at approval), and registration metadata columns.

-- 1. Drop the old status CHECK constraint (must recreate to add values)
ALTER TABLE center_schema.centers DROP CONSTRAINT chk_centers_status;

-- 2. Recreate with new statuses
ALTER TABLE center_schema.centers
    ADD CONSTRAINT chk_centers_status
        CHECK (status IN ('ACTIVE','SUSPENDED','CLOSED','PENDING_VERIFICATION','REJECTED'));

-- 3. Make code nullable — PENDING centers have no code until approved
ALTER TABLE center_schema.centers ALTER COLUMN code DROP NOT NULL;

-- 4. Registration metadata columns
ALTER TABLE center_schema.centers ADD COLUMN IF NOT EXISTS institution_type  VARCHAR(50);
ALTER TABLE center_schema.centers ADD COLUMN IF NOT EXISTS board             VARCHAR(100);
ALTER TABLE center_schema.centers ADD COLUMN IF NOT EXISTS admin_user_id     UUID;
ALTER TABLE center_schema.centers ADD COLUMN IF NOT EXISTS registration_source VARCHAR(30) NOT NULL DEFAULT 'ADMIN_CREATED';
ALTER TABLE center_schema.centers ADD COLUMN IF NOT EXISTS rejection_reason  TEXT;
ALTER TABLE center_schema.centers ADD COLUMN IF NOT EXISTS reviewed_by       UUID;
ALTER TABLE center_schema.centers ADD COLUMN IF NOT EXISTS reviewed_at       TIMESTAMPTZ;

-- 5. Indexes for registration queries
CREATE INDEX IF NOT EXISTS idx_centers_admin_user_id
    ON center_schema.centers (admin_user_id)
    WHERE admin_user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_centers_status_source
    ON center_schema.centers (status, registration_source)
    WHERE deleted_at IS NULL;
