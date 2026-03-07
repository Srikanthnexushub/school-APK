-- V4__create_exam_enrollments.sql
CREATE TABLE assess_schema.exam_enrollments (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id     UUID        NOT NULL REFERENCES assess_schema.exams(id),
    student_id  UUID        NOT NULL,
    status      TEXT        NOT NULL DEFAULT 'ENROLLED',
    version     BIGINT      NOT NULL DEFAULT 0,
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_enrollment_status CHECK (status IN ('ENROLLED', 'WITHDRAWN'))
);

-- A student can only be enrolled once per exam (regardless of status)
CREATE UNIQUE INDEX uq_exam_enrollments_active
    ON assess_schema.exam_enrollments(exam_id, student_id)
    WHERE status = 'ENROLLED';

CREATE INDEX idx_exam_enrollments_exam_id
    ON assess_schema.exam_enrollments(exam_id);

CREATE INDEX idx_exam_enrollments_student_id
    ON assess_schema.exam_enrollments(student_id);

CREATE TRIGGER trg_exam_enrollments_updated_at
    BEFORE UPDATE ON assess_schema.exam_enrollments
    FOR EACH ROW EXECUTE FUNCTION assess_schema.set_updated_at();
