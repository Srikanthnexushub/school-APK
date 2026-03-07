-- V2: Create exam_enrollments table
CREATE TABLE examtracker_schema.exam_enrollments (
    id              UUID        NOT NULL DEFAULT uuid_generate_v4(),
    student_id      UUID        NOT NULL,
    exam_code       VARCHAR(30) NOT NULL,
    exam_name       VARCHAR(200) NOT NULL,
    exam_date       DATE,
    target_year     INTEGER     NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_exam_enrollments PRIMARY KEY (id),
    CONSTRAINT chk_exam_enrollments_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'DROPPED')),
    CONSTRAINT chk_exam_enrollments_target_year CHECK (target_year >= 2020)
);

-- Index for querying enrollments by student
CREATE INDEX idx_exam_enrollments_student_id
    ON examtracker_schema.exam_enrollments (student_id)
    WHERE deleted_at IS NULL;

-- Index for filtering by exam code
CREATE INDEX idx_exam_enrollments_exam_code
    ON examtracker_schema.exam_enrollments (exam_code)
    WHERE deleted_at IS NULL;

-- Partial unique index: a student can only have one active enrollment per exam
CREATE UNIQUE INDEX uq_exam_enrollments_student_exam
    ON examtracker_schema.exam_enrollments (student_id, exam_code)
    WHERE deleted_at IS NULL;
