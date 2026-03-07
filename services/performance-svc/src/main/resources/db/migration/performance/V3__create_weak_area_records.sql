-- V3: Create weak_area_records table

CREATE TABLE performance_schema.weak_area_records (
    id                    UUID         NOT NULL DEFAULT uuid_generate_v4(),
    student_id            UUID         NOT NULL,
    enrollment_id         UUID         NOT NULL,
    subject               VARCHAR(255) NOT NULL,
    topic_name            VARCHAR(255) NOT NULL,
    chapter_name          VARCHAR(255),
    mastery_percent       NUMERIC(5,2) NOT NULL,
    primary_error_type    VARCHAR(50)  NOT NULL,
    incorrect_attempts    INTEGER      NOT NULL DEFAULT 0,
    total_attempts        INTEGER      NOT NULL DEFAULT 0,
    prerequisites_weak    BOOLEAN      NOT NULL DEFAULT FALSE,
    detected_at           TIMESTAMPTZ  NOT NULL,
    last_reviewed_at      TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ,
    deleted_at            TIMESTAMPTZ,
    version               BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_weak_area_records PRIMARY KEY (id)
);

CREATE INDEX idx_weak_area_records_student_enrollment
    ON performance_schema.weak_area_records (student_id, enrollment_id);

CREATE INDEX idx_weak_area_records_mastery_active
    ON performance_schema.weak_area_records (mastery_percent)
    WHERE deleted_at IS NULL;
