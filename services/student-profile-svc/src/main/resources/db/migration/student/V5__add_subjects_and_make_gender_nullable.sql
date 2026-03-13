-- V5: Add subjects column and make gender nullable (not collected at registration time)
ALTER TABLE student_schema.students ADD COLUMN IF NOT EXISTS subjects JSONB NOT NULL DEFAULT '[]';
ALTER TABLE student_schema.students ALTER COLUMN gender DROP NOT NULL;
