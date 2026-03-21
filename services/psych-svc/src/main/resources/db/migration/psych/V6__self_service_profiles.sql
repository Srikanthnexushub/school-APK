-- V6: Self-service psychometric profiles
-- Allow students/parents to create their own profile without admin activation.
-- center_id and batch_id become optional (NULL = self-activated, no center assignment).

ALTER TABLE psych_schema.psych_profiles
    ALTER COLUMN center_id DROP NOT NULL,
    ALTER COLUMN batch_id  DROP NOT NULL;

-- Old unique constraint was (student_id, center_id) — allowed same student at multiple centers.
-- New rule: one active profile per person (student_id), regardless of center.
ALTER TABLE psych_schema.psych_profiles
    DROP CONSTRAINT IF EXISTS uq_psych_profile_student_center;

CREATE UNIQUE INDEX IF NOT EXISTS uq_psych_profile_student
    ON psych_schema.psych_profiles(student_id)
    WHERE deleted_at IS NULL;
