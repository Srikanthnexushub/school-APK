CREATE TABLE mentor_schema.mentor_sessions (
    id UUID PRIMARY KEY,
    mentor_id UUID NOT NULL REFERENCES mentor_schema.mentor_profiles(id),
    student_id UUID NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL DEFAULT 60,
    session_mode VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    meeting_link VARCHAR(500),
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_sessions_mentor ON mentor_schema.mentor_sessions(mentor_id, scheduled_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_sessions_student ON mentor_schema.mentor_sessions(student_id) WHERE deleted_at IS NULL;
