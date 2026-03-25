-- V13__fix_enrollment_status_constraint.sql
-- The Java domain model has EnrollmentStatus.COMPLETED but the DB constraint only allowed
-- ENROLLED and WITHDRAWN, causing a DataIntegrityViolationException on exam submission.
ALTER TABLE assess_schema.exam_enrollments
    DROP CONSTRAINT chk_enrollment_status;

ALTER TABLE assess_schema.exam_enrollments
    ADD CONSTRAINT chk_enrollment_status CHECK (status IN ('ENROLLED', 'WITHDRAWN', 'COMPLETED'));
