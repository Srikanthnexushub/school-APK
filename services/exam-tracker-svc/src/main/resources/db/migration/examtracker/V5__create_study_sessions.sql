-- V5: Create study_sessions table
CREATE TABLE examtracker_schema.study_sessions (
    id                  UUID            NOT NULL DEFAULT uuid_generate_v4(),
    student_id          UUID            NOT NULL,
    enrollment_id       UUID            NOT NULL,
    subject             VARCHAR(100)    NOT NULL,
    topic_name          VARCHAR(200)    NOT NULL,
    session_type        VARCHAR(30)     NOT NULL,
    session_date        DATE            NOT NULL,
    duration_minutes    INTEGER         NOT NULL,
    questions_attempted INTEGER,
    accuracy_percent    NUMERIC(5, 2),
    notes               TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    version             BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_study_sessions PRIMARY KEY (id),
    CONSTRAINT chk_study_sessions_session_type CHECK (session_type IN ('PRACTICE', 'MOCK_TEST', 'REVISION', 'CONCEPT_LEARNING')),
    CONSTRAINT chk_study_sessions_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_study_sessions_accuracy CHECK (accuracy_percent IS NULL OR (accuracy_percent >= 0 AND accuracy_percent <= 100)),
    CONSTRAINT fk_study_sessions_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES examtracker_schema.exam_enrollments (id)
);

-- Composite index for querying sessions by student and enrollment
CREATE INDEX idx_study_sessions_student_enrollment
    ON examtracker_schema.study_sessions (student_id, enrollment_id)
    WHERE deleted_at IS NULL;

-- BRIN index on session_date for time-range queries (efficient for append-only data)
CREATE INDEX idx_study_sessions_date_brin
    ON examtracker_schema.study_sessions USING BRIN (session_date);
