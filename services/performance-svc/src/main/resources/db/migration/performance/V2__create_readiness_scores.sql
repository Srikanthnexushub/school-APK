-- V2: Create readiness_scores table

CREATE TABLE performance_schema.readiness_scores (
    id                       UUID         NOT NULL DEFAULT uuid_generate_v4(),
    student_id               UUID         NOT NULL,
    enrollment_id            UUID         NOT NULL,
    ers_score                NUMERIC(5,2) NOT NULL,
    syllabus_coverage_percent NUMERIC(5,2) NOT NULL,
    mock_test_trend_score    NUMERIC(5,2) NOT NULL,
    mastery_average          NUMERIC(5,2) NOT NULL,
    time_management_score    NUMERIC(5,2) NOT NULL,
    accuracy_consistency     NUMERIC(5,2) NOT NULL,
    projected_rank           INTEGER,
    projected_percentile     NUMERIC(5,2),
    computed_at              TIMESTAMPTZ  NOT NULL,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at               TIMESTAMPTZ,
    version                  BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_readiness_scores PRIMARY KEY (id)
);

CREATE INDEX idx_readiness_scores_student_enrollment
    ON performance_schema.readiness_scores (student_id, enrollment_id);

CREATE INDEX brin_readiness_scores_computed_at
    ON performance_schema.readiness_scores USING BRIN (computed_at);
