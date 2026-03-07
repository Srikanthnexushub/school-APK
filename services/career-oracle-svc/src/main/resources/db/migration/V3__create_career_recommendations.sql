CREATE TABLE careeroracle_schema.career_recommendations (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    career_stream VARCHAR(50) NOT NULL,
    fit_score NUMERIC(5,2) NOT NULL,
    confidence_level VARCHAR(20) NOT NULL,
    rationale TEXT NOT NULL,
    rank_order INTEGER NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    valid_until TIMESTAMPTZ,
    is_active BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_career_recs_student ON careeroracle_schema.career_recommendations(student_id, rank_order) WHERE is_active = true;
