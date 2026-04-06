-- V35: Add grade column to batches to support grade-specific batch assignment
-- Nullable: coaching centers may not require a grade (subject-only batches remain valid)
ALTER TABLE center_schema.batches
    ADD COLUMN IF NOT EXISTS grade VARCHAR(10);

COMMENT ON COLUMN center_schema.batches.grade IS
    'Grade/class/year for this batch: 1-12 for school classes, Y1-Y4 for college years, or null for coaching center subject batches';
