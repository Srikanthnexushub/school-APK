CREATE TABLE assess_schema.assignments (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id             UUID         NOT NULL,
    center_id            UUID         NOT NULL,
    created_by_user_id   UUID         NOT NULL,
    title                TEXT         NOT NULL,
    description          TEXT,
    type                 TEXT         NOT NULL DEFAULT 'HOMEWORK',
    due_date             TIMESTAMPTZ,
    total_marks          NUMERIC(8,2) NOT NULL CHECK (total_marks > 0),
    passing_marks        NUMERIC(8,2) NOT NULL CHECK (passing_marks >= 0),
    instructions         TEXT,
    attachment_url       TEXT,
    status               TEXT         NOT NULL DEFAULT 'DRAFT',
    version              BIGINT       NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ,
    CONSTRAINT chk_assignment_type   CHECK (type   IN ('HOMEWORK','CLASSWORK','PROJECT','QUIZ','PRACTICE')),
    CONSTRAINT chk_assignment_status CHECK (status IN ('DRAFT','PUBLISHED','CLOSED','CANCELLED')),
    CONSTRAINT chk_assignment_passing_lte_total CHECK (passing_marks <= total_marks)
);
CREATE INDEX idx_assignments_batch_id   ON assess_schema.assignments (batch_id)  WHERE deleted_at IS NULL;
CREATE INDEX idx_assignments_center_id  ON assess_schema.assignments (center_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_assignments_created_by ON assess_schema.assignments (created_by_user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_assignments_status     ON assess_schema.assignments (status)    WHERE deleted_at IS NULL;
