-- V24__create_batch_members.sql
-- Mirrors student-batch membership from student-profile-svc via Kafka events.
-- Used by attendance module to provide teacher with student roster per batch.
CREATE TABLE center_schema.batch_members (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    batch_id     UUID        NOT NULL,
    center_id    UUID        NOT NULL,
    student_id   UUID        NOT NULL,
    student_name VARCHAR(200),
    enrolled_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    withdrawn_at TIMESTAMPTZ,
    CONSTRAINT pk_batch_members         PRIMARY KEY (id),
    CONSTRAINT uq_batch_member          UNIQUE (batch_id, student_id)
);

CREATE INDEX idx_batch_members_batch   ON center_schema.batch_members(batch_id)   WHERE withdrawn_at IS NULL;
CREATE INDEX idx_batch_members_student ON center_schema.batch_members(student_id);
CREATE INDEX idx_batch_members_center  ON center_schema.batch_members(center_id);
