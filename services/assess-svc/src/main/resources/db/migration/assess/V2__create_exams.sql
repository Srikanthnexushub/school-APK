-- V2__create_exams.sql
CREATE TABLE assess_schema.exams (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id         UUID          NOT NULL,
    center_id        UUID          NOT NULL,
    title            TEXT          NOT NULL,
    description      TEXT,
    mode             TEXT          NOT NULL DEFAULT 'STANDARD',
    duration_minutes INT           NOT NULL CHECK (duration_minutes > 0),
    max_attempts     INT           NOT NULL DEFAULT 1 CHECK (max_attempts > 0),
    start_at         TIMESTAMPTZ,
    end_at           TIMESTAMPTZ,
    total_marks      NUMERIC(8,2)  NOT NULL CHECK (total_marks > 0),
    passing_marks    NUMERIC(8,2)  NOT NULL CHECK (passing_marks >= 0),
    status           TEXT          NOT NULL DEFAULT 'DRAFT',
    version          BIGINT        NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ,
    CONSTRAINT chk_exam_mode   CHECK (mode   IN ('STANDARD', 'CAT')),
    CONSTRAINT chk_exam_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT chk_passing_lte_total CHECK (passing_marks <= total_marks)
);

CREATE INDEX idx_exams_batch_id   ON assess_schema.exams(batch_id)   WHERE deleted_at IS NULL;
CREATE INDEX idx_exams_center_id  ON assess_schema.exams(center_id)  WHERE deleted_at IS NULL;
CREATE INDEX idx_exams_status     ON assess_schema.exams(status)      WHERE deleted_at IS NULL;

CREATE OR REPLACE FUNCTION assess_schema.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_exams_updated_at
    BEFORE UPDATE ON assess_schema.exams
    FOR EACH ROW EXECUTE FUNCTION assess_schema.set_updated_at();
