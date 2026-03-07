CREATE TABLE mentor_schema.mentor_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    bio TEXT,
    specializations VARCHAR(500),
    years_of_experience INTEGER NOT NULL DEFAULT 0,
    hourly_rate NUMERIC(8,2),
    is_available BOOLEAN NOT NULL DEFAULT true,
    average_rating NUMERIC(3,2) DEFAULT 0.00,
    total_sessions INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
