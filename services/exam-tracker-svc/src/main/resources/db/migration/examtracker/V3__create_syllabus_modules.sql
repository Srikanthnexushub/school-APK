-- V3: Create syllabus_modules table
CREATE TABLE examtracker_schema.syllabus_modules (
    id                  UUID        NOT NULL DEFAULT uuid_generate_v4(),
    enrollment_id       UUID        NOT NULL,
    student_id          UUID        NOT NULL,
    subject             VARCHAR(100) NOT NULL,
    topic_name          VARCHAR(200) NOT NULL,
    chapter_name        VARCHAR(200) NOT NULL,
    weightage_percent   INTEGER     NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    completion_percent  INTEGER     NOT NULL DEFAULT 0,
    last_studied_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    version             BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_syllabus_modules PRIMARY KEY (id),
    CONSTRAINT chk_syllabus_modules_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'REVISION')),
    CONSTRAINT chk_syllabus_modules_weightage CHECK (weightage_percent >= 0 AND weightage_percent <= 100),
    CONSTRAINT chk_syllabus_modules_completion CHECK (completion_percent >= 0 AND completion_percent <= 100),
    CONSTRAINT fk_syllabus_modules_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES examtracker_schema.exam_enrollments (id)
);

-- Index for querying modules by enrollment
CREATE INDEX idx_syllabus_modules_enrollment_id
    ON examtracker_schema.syllabus_modules (enrollment_id)
    WHERE deleted_at IS NULL;

-- Composite index for querying by student and subject
CREATE INDEX idx_syllabus_modules_student_subject
    ON examtracker_schema.syllabus_modules (student_id, subject)
    WHERE deleted_at IS NULL;
