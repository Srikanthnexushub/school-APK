-- V11: Widen current_class range from 10–13 to 1–13 to support all school grades (1–12) and college years (with 13 = dropper)
ALTER TABLE student_schema.students
    DROP CONSTRAINT IF EXISTS students_current_class_check;

ALTER TABLE student_schema.students
    ADD CONSTRAINT students_current_class_check
    CHECK (current_class BETWEEN 1 AND 13);
