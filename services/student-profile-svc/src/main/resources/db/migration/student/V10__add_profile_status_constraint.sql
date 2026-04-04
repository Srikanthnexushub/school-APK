-- V10: Add CHECK constraint for ProfileStatus enum values
-- Enum values verified from ProfileStatus.java: ACTIVE, INACTIVE, SUSPENDED

ALTER TABLE student_schema.students
    DROP CONSTRAINT IF EXISTS students_status_check;

ALTER TABLE student_schema.students
    ADD CONSTRAINT students_status_check
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'));
