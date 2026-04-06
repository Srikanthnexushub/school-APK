-- V12: Add college_program column for college students (BTech, MBBS, BCA, etc.)
ALTER TABLE student_schema.students
    ADD COLUMN IF NOT EXISTS college_program VARCHAR(50);
