-- V5__create_submissions.sql
CREATE TABLE assess_schema.submissions (
    id             UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id        UUID             NOT NULL REFERENCES assess_schema.exams(id),
    student_id     UUID             NOT NULL,
    enrollment_id  UUID             NOT NULL REFERENCES assess_schema.exam_enrollments(id),
    attempt_number INT              NOT NULL CHECK (attempt_number > 0),
    started_at     TIMESTAMPTZ      NOT NULL DEFAULT now(),
    submitted_at   TIMESTAMPTZ,
    total_marks    DOUBLE PRECISION NOT NULL DEFAULT 0,
    scored_marks   DOUBLE PRECISION NOT NULL DEFAULT 0,
    percentage     DOUBLE PRECISION NOT NULL DEFAULT 0,
    status         TEXT             NOT NULL DEFAULT 'IN_PROGRESS',
    version        BIGINT           NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT chk_submission_status CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'GRADED', 'INVALIDATED'))
);

CREATE UNIQUE INDEX uq_submissions_attempt
    ON assess_schema.submissions(exam_id, student_id, attempt_number);

CREATE INDEX idx_submissions_exam_student
    ON assess_schema.submissions(exam_id, student_id);

CREATE INDEX idx_submissions_student_id
    ON assess_schema.submissions(student_id);

CREATE TRIGGER trg_submissions_updated_at
    BEFORE UPDATE ON assess_schema.submissions
    FOR EACH ROW EXECUTE FUNCTION assess_schema.set_updated_at();
