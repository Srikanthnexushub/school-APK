-- V11__add_branch_to_centers.sql
-- Adds branch field to centers for institution self-registration
ALTER TABLE center_schema.centers ADD COLUMN IF NOT EXISTS branch VARCHAR(200);
