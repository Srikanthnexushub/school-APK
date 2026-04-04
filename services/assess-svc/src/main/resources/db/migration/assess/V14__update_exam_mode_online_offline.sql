-- V14: Rename exam modes STANDARD→ONLINE, CAT→OFFLINE
-- Drop old check constraint, migrate data, add new constraint

ALTER TABLE assess_schema.exams DROP CONSTRAINT IF EXISTS chk_exam_mode;

UPDATE assess_schema.exams SET mode = 'ONLINE'  WHERE mode = 'STANDARD';
UPDATE assess_schema.exams SET mode = 'OFFLINE' WHERE mode = 'CAT';

ALTER TABLE assess_schema.exams ALTER COLUMN mode SET DEFAULT 'ONLINE';

ALTER TABLE assess_schema.exams
    ADD CONSTRAINT chk_exam_mode CHECK (mode IN ('ONLINE', 'OFFLINE'));
