ALTER TABLE parent_schema.student_links
    ADD COLUMN IF NOT EXISTS date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS school_name TEXT,
    ADD COLUMN IF NOT EXISTS standard INTEGER,
    ADD COLUMN IF NOT EXISTS board TEXT,
    ADD COLUMN IF NOT EXISTS roll_number TEXT;
