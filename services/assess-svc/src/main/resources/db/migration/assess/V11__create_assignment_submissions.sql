CREATE TABLE assess_schema.assignment_submissions (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id   UUID         NOT NULL REFERENCES assess_schema.assignments (id),
    student_id      UUID         NOT NULL,
    text_response   TEXT,
    score           NUMERIC(8,2) CHECK (score >= 0),
    feedback        TEXT,
    status          TEXT         NOT NULL DEFAULT 'PENDING',
    submitted_at    TIMESTAMPTZ,
    graded_at       TIMESTAMPTZ,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_submission_status CHECK (status IN ('PENDING','SUBMITTED','LATE','GRADED')),
    CONSTRAINT uq_submission_student UNIQUE (assignment_id, student_id)
);
CREATE INDEX idx_asub_assignment_id ON assess_schema.assignment_submissions (assignment_id);
CREATE INDEX idx_asub_student_id    ON assess_schema.assignment_submissions (student_id);
