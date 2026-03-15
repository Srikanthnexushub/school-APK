-- V13: Add district and country fields to parent_profiles
ALTER TABLE parent_schema.parent_profiles ADD COLUMN IF NOT EXISTS district TEXT;
ALTER TABLE parent_schema.parent_profiles ADD COLUMN IF NOT EXISTS country TEXT;
