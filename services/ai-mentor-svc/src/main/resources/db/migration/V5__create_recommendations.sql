CREATE TABLE aimentor_schema.recommendations (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    subject_area VARCHAR(50) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    recommendation_text TEXT NOT NULL,
    priority_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    is_acknowledged BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ
);
CREATE INDEX idx_recommendations_student ON aimentor_schema.recommendations(student_id) WHERE is_acknowledged = false;
