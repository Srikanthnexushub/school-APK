-- V6: Add parent_link_code column for parent-student linking via 6-digit code
ALTER TABLE student_schema.students
    ADD COLUMN IF NOT EXISTS parent_link_code VARCHAR(6);

-- Backfill existing rows with unique random 6-digit codes
DO $$
DECLARE
    rec RECORD;
    candidate TEXT;
BEGIN
    FOR rec IN SELECT id FROM student_schema.students WHERE parent_link_code IS NULL LOOP
        LOOP
            candidate := LPAD((FLOOR(RANDOM() * 1000000))::INT::TEXT, 6, '0');
            EXIT WHEN NOT EXISTS (
                SELECT 1 FROM student_schema.students WHERE parent_link_code = candidate
            );
        END LOOP;
        UPDATE student_schema.students SET parent_link_code = candidate WHERE id = rec.id;
    END LOOP;
END $$;

ALTER TABLE student_schema.students
    ALTER COLUMN parent_link_code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_students_parent_link_code
    ON student_schema.students(parent_link_code) WHERE deleted_at IS NULL;
