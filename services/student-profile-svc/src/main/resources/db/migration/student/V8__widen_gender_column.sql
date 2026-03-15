-- V8: Widen gender column to support PREFER_NOT_TO_SAY value
ALTER TABLE student_schema.students ALTER COLUMN gender TYPE VARCHAR(20);
