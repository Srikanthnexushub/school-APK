-- V29: Add country field to teachers and centers tables.
-- Teachers can now store their country during self-registration.
-- Centers (institutions) can store their country during self-registration.
ALTER TABLE center_schema.teachers ADD COLUMN IF NOT EXISTS country VARCHAR(100);
ALTER TABLE center_schema.centers  ADD COLUMN IF NOT EXISTS country VARCHAR(100);
