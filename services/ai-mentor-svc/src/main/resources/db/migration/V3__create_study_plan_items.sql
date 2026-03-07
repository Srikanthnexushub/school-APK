CREATE TABLE aimentor_schema.study_plan_items (
    id UUID PRIMARY KEY,
    study_plan_id UUID NOT NULL REFERENCES aimentor_schema.study_plans(id),
    subject_area VARCHAR(50) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    priority_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    interval_days INTEGER NOT NULL DEFAULT 1,
    repetitions INTEGER NOT NULL DEFAULT 0,
    ease_factor NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    next_review_at DATE,
    last_reviewed_at DATE,
    quality INTEGER,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_plan_items_plan ON aimentor_schema.study_plan_items(study_plan_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_plan_items_review ON aimentor_schema.study_plan_items(study_plan_id, next_review_at) WHERE deleted_at IS NULL;
