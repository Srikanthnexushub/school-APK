-- V11: Add occupation to parent_profiles; add per-child relationship to student_links
ALTER TABLE parent_schema.parent_profiles
    ADD COLUMN IF NOT EXISTS occupation TEXT;

ALTER TABLE parent_schema.student_links
    ADD COLUMN IF NOT EXISTS relationship TEXT DEFAULT 'PARENT';
