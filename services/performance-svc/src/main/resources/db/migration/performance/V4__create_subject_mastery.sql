-- V4: Create subject_mastery table

CREATE TABLE performance_schema.subject_mastery (
    id                UUID         NOT NULL DEFAULT uuid_generate_v4(),
    student_id        UUID         NOT NULL,
    enrollment_id     UUID         NOT NULL,
    subject           VARCHAR(255) NOT NULL,
    mastery_percent   NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    mastery_level     VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
    velocity_per_week NUMERIC(7,4),
    total_topics      INTEGER      NOT NULL DEFAULT 0,
    mastered_topics   INTEGER      NOT NULL DEFAULT 0,
    last_updated_at   TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ,
    version           BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_subject_mastery PRIMARY KEY (id)
);

CREATE UNIQUE INDEX udx_subject_mastery_student_enrollment_subject
    ON performance_schema.subject_mastery (student_id, enrollment_id, subject);
