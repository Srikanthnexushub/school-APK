-- Fix #96: total_marks and passing_marks are no longer required on assignment creation.
-- Drop NOT NULL constraints so existing rows are unaffected and new rows can omit marks.
ALTER TABLE assess_schema.assignments ALTER COLUMN total_marks DROP NOT NULL;
ALTER TABLE assess_schema.assignments ALTER COLUMN passing_marks DROP NOT NULL;
