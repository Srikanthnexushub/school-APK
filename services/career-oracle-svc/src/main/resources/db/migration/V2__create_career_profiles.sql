CREATE TABLE careeroracle_schema.career_profiles (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL UNIQUE,
    enrollment_id UUID NOT NULL,
    academic_stream VARCHAR(50) NOT NULL,
    current_grade INTEGER NOT NULL,
    ers_score NUMERIC(5,2),
    preferred_career_stream VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
