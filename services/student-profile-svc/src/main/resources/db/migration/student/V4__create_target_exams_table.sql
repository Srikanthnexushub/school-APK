-- V4: Create target_exams table
CREATE TABLE student_schema.target_exams (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    student_id  UUID        NOT NULL REFERENCES student_schema.students(id),
    exam_code   VARCHAR(30) NOT NULL,
    target_year INTEGER     NOT NULL,
    priority    INTEGER     NOT NULL DEFAULT 1,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_target_exams_student_id  ON student_schema.target_exams (student_id);
CREATE INDEX idx_target_exams_deleted_at  ON student_schema.target_exams (deleted_at);

-- Partial unique index: enforce uniqueness of (student_id, exam_code) for active (non-deleted) rows only
CREATE UNIQUE INDEX uq_target_exams_student_exam_active
    ON student_schema.target_exams (student_id, exam_code)
    WHERE deleted_at IS NULL;
