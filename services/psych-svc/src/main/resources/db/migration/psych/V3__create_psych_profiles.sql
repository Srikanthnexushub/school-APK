CREATE TABLE psych_schema.psych_profiles (
    id                UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id        UUID             NOT NULL,
    center_id         UUID             NOT NULL,
    batch_id          UUID             NOT NULL,
    openness          DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (openness BETWEEN 0.0 AND 1.0),
    conscientiousness DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (conscientiousness BETWEEN 0.0 AND 1.0),
    extraversion      DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (extraversion BETWEEN 0.0 AND 1.0),
    agreeableness     DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (agreeableness BETWEEN 0.0 AND 1.0),
    neuroticism       DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (neuroticism BETWEEN 0.0 AND 1.0),
    riasec_code       TEXT,
    embedding_json    TEXT,   -- placeholder for pgvector; upgrade: ALTER COLUMN to vector(1536)
    status            TEXT             NOT NULL DEFAULT 'DRAFT',
    version           BIGINT           NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ      NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,
    CONSTRAINT chk_pp_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT uq_psych_profile_student_center UNIQUE (student_id, center_id)
);

CREATE INDEX idx_psych_profiles_center_id  ON psych_schema.psych_profiles(center_id)  WHERE deleted_at IS NULL;
CREATE INDEX idx_psych_profiles_student_id ON psych_schema.psych_profiles(student_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_psych_profiles_status     ON psych_schema.psych_profiles(status)     WHERE deleted_at IS NULL;

CREATE OR REPLACE FUNCTION psych_schema.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_psych_profiles_updated_at
    BEFORE UPDATE ON psych_schema.psych_profiles
    FOR EACH ROW EXECUTE FUNCTION psych_schema.set_updated_at();
