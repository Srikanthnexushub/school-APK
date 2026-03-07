-- V3__create_batches_table.sql
CREATE TABLE center_schema.batches (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    center_id       UUID        NOT NULL,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(50) NOT NULL,
    subject         VARCHAR(100) NOT NULL,
    teacher_id      UUID,
    max_students    INT         NOT NULL CHECK (max_students > 0),
    enrolled_count  INT         NOT NULL DEFAULT 0 CHECK (enrolled_count >= 0),
    start_date      DATE        NOT NULL,
    end_date        DATE,
    status          VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    version         BIGINT      NOT NULL DEFAULT 0,

    CONSTRAINT pk_batches        PRIMARY KEY (id),
    CONSTRAINT fk_batches_center FOREIGN KEY (center_id) REFERENCES center_schema.centers (id),
    CONSTRAINT chk_batches_status CHECK (status IN ('UPCOMING','ACTIVE','COMPLETED','CANCELLED')),
    CONSTRAINT chk_batches_enrollment CHECK (enrolled_count <= max_students)
);

CREATE INDEX idx_batches_center_id ON center_schema.batches (center_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_batches_status    ON center_schema.batches (status)    WHERE deleted_at IS NULL;
CREATE INDEX idx_batches_teacher_id ON center_schema.batches (teacher_id) WHERE teacher_id IS NOT NULL AND deleted_at IS NULL;

CREATE TRIGGER trg_batches_updated_at
    BEFORE UPDATE ON center_schema.batches
    FOR EACH ROW EXECUTE FUNCTION center_schema.set_updated_at();
