-- V10__teacher_bulk_import.sql
-- Teacher Onboarding (3.4): bulk CSV import + self-registration with approval flow

-- Make user_id nullable so bulk-import stubs can exist before the teacher accepts their invitation
ALTER TABLE center_schema.teachers
    ALTER COLUMN user_id DROP NOT NULL;

-- Extra columns for bulk import and invitation flow
ALTER TABLE center_schema.teachers
    ADD COLUMN employee_id              VARCHAR(50),
    ADD COLUMN invitation_token         VARCHAR(255),
    ADD COLUMN invitation_token_expires_at TIMESTAMPTZ;

-- Expand allowed status values
ALTER TABLE center_schema.teachers
    DROP CONSTRAINT chk_teachers_status;
ALTER TABLE center_schema.teachers
    ADD CONSTRAINT chk_teachers_status
        CHECK (status IN ('ACTIVE','INACTIVE','PENDING_APPROVAL','INVITATION_SENT'));

-- Fast token lookup (unique, sparse)
CREATE UNIQUE INDEX idx_teachers_invitation_token
    ON center_schema.teachers (invitation_token)
    WHERE invitation_token IS NOT NULL;

-- Fast listing of pending-approval teachers per center
CREATE INDEX idx_teachers_pending
    ON center_schema.teachers (center_id, status)
    WHERE status = 'PENDING_APPROVAL' AND deleted_at IS NULL;
