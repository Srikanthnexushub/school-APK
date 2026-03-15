-- V14: Make center_id and student_name nullable in student_links to support student-initiated links
ALTER TABLE parent_schema.student_links ALTER COLUMN center_id DROP NOT NULL;
ALTER TABLE parent_schema.student_links ALTER COLUMN student_name DROP NOT NULL;
