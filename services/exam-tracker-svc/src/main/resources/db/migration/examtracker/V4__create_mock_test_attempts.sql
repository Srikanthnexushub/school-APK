-- V4: Create mock_test_attempts table
CREATE TABLE examtracker_schema.mock_test_attempts (
    id                  UUID            NOT NULL DEFAULT uuid_generate_v4(),
    student_id          UUID            NOT NULL,
    enrollment_id       UUID            NOT NULL,
    test_name           VARCHAR(300)    NOT NULL,
    exam_code           VARCHAR(30)     NOT NULL,
    attempt_date        DATE            NOT NULL,
    total_questions     INTEGER         NOT NULL,
    attempted           INTEGER         NOT NULL,
    correct             INTEGER         NOT NULL,
    incorrect           INTEGER         NOT NULL,
    score               NUMERIC(10, 2)  NOT NULL,
    max_score           NUMERIC(10, 2)  NOT NULL,
    accuracy_percent    NUMERIC(5, 2)   NOT NULL,
    time_taken_minutes  INTEGER         NOT NULL,
    total_time_minutes  INTEGER         NOT NULL,
    estimated_rank      INTEGER,
    subject_wise_json   TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    version             BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_mock_test_attempts PRIMARY KEY (id),
    CONSTRAINT chk_mock_test_attempts_questions CHECK (total_questions > 0),
    CONSTRAINT chk_mock_test_attempts_attempted CHECK (attempted >= 0 AND attempted <= total_questions),
    CONSTRAINT chk_mock_test_attempts_accuracy CHECK (accuracy_percent >= 0 AND accuracy_percent <= 100),
    CONSTRAINT fk_mock_test_attempts_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES examtracker_schema.exam_enrollments (id)
);

-- Composite index for querying attempts by student and enrollment
CREATE INDEX idx_mock_test_attempts_student_enrollment
    ON examtracker_schema.mock_test_attempts (student_id, enrollment_id)
    WHERE deleted_at IS NULL;

-- BRIN index on attempt_date for time-range queries (efficient for append-only data)
CREATE INDEX idx_mock_test_attempts_date_brin
    ON examtracker_schema.mock_test_attempts USING BRIN (attempt_date);
