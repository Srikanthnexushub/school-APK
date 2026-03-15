-- V6: add district to mentor_profiles
ALTER TABLE mentor_schema.mentor_profiles
    ADD COLUMN IF NOT EXISTS district VARCHAR(100);
