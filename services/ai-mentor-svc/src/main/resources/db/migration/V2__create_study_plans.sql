CREATE TABLE aimentor_schema.study_plans (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL,
    enrollment_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_exam_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_study_plans_student ON aimentor_schema.study_plans(student_id) WHERE deleted_at IS NULL;
