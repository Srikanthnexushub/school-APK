CREATE TABLE aimentor_schema.doubt_tickets (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    subject_area VARCHAR(50) NOT NULL,
    question TEXT NOT NULL,
    ai_answer TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_doubt_tickets_student ON aimentor_schema.doubt_tickets(student_id, status) WHERE deleted_at IS NULL;
