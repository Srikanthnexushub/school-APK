-- V6: Add assess_exam_id to exam_enrollments so that exam-tracker can auto-sync
--     completion events published by assess-svc when a student submits an institutional exam.
--     This column is nullable because existing enrollments (competitive exam tracking) do not
--     have a corresponding assess-svc exam.

ALTER TABLE examtracker_schema.exam_enrollments
    ADD COLUMN assess_exam_id UUID;

-- Unique index: one auto-sync enrollment per student per assess exam
CREATE UNIQUE INDEX uq_exam_enrollments_student_assess_exam
    ON examtracker_schema.exam_enrollments (student_id, assess_exam_id)
    WHERE assess_exam_id IS NOT NULL AND deleted_at IS NULL;

-- Index for fast lookup by assess_exam_id (used by consumer upsert query)
CREATE INDEX idx_exam_enrollments_assess_exam_id
    ON examtracker_schema.exam_enrollments (assess_exam_id)
    WHERE assess_exam_id IS NOT NULL;
