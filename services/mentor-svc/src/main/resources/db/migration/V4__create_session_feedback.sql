CREATE TABLE mentor_schema.session_feedback (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES mentor_schema.mentor_sessions(id),
    student_id UUID NOT NULL,
    mentor_id UUID NOT NULL,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_feedback_session ON mentor_schema.session_feedback(session_id);
