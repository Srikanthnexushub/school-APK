CREATE TABLE psych_schema.career_mappings (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id    UUID         NOT NULL REFERENCES psych_schema.psych_profiles(id),
    student_id    UUID         NOT NULL,
    center_id     UUID         NOT NULL,
    requested_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    generated_at  TIMESTAMPTZ,
    top_careers   TEXT,        -- JSON array: ["Software Engineer", "Data Scientist", ...]
    reasoning     TEXT,        -- AI reasoning text
    model_version TEXT,
    status        TEXT         NOT NULL DEFAULT 'PENDING',
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT chk_cm_status CHECK (status IN ('PENDING', 'GENERATED', 'FAILED'))
);

CREATE INDEX idx_cm_profile_id  ON psych_schema.career_mappings(profile_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_cm_student_id  ON psych_schema.career_mappings(student_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_cm_status      ON psych_schema.career_mappings(status)     WHERE deleted_at IS NULL;

-- BRIN index on requested_at (append-heavy)
CREATE INDEX idx_cm_requested_brin ON psych_schema.career_mappings USING BRIN(requested_at);

CREATE TRIGGER trg_career_mappings_updated_at
    BEFORE UPDATE ON psych_schema.career_mappings
    FOR EACH ROW EXECUTE FUNCTION psych_schema.set_updated_at();
