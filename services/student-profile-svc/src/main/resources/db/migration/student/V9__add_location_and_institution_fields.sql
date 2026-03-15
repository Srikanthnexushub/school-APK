-- V9: Add district, country and institution_name fields to students
ALTER TABLE student_schema.students ADD COLUMN IF NOT EXISTS district VARCHAR(100);
ALTER TABLE student_schema.students ADD COLUMN IF NOT EXISTS country VARCHAR(100);
ALTER TABLE student_schema.students ADD COLUMN IF NOT EXISTS institution_name VARCHAR(255);
