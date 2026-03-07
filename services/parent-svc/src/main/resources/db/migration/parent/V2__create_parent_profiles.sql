-- V2__create_parent_profiles.sql
-- Parent profile: one per user account (userId from auth-svc)

CREATE TABLE parent_schema.parent_profiles (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    name        TEXT        NOT NULL,
    phone       TEXT,
    verified    BOOLEAN     NOT NULL DEFAULT FALSE,
    status      TEXT        NOT NULL DEFAULT 'ACTIVE',
    version     BIGINT      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT chk_parent_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

-- A user can have at most one active parent profile
CREATE UNIQUE INDEX uq_parent_profiles_user_id
    ON parent_schema.parent_profiles(user_id)
    WHERE deleted_at IS NULL;

-- Lookup index for getMyProfile queries
CREATE INDEX idx_parent_profiles_user_id_active
    ON parent_schema.parent_profiles(user_id)
    WHERE deleted_at IS NULL;

-- BRIN for append-heavy time-ordered queries
CREATE INDEX idx_parent_profiles_created_brin
    ON parent_schema.parent_profiles USING BRIN(created_at);

-- Auto-update trigger for updated_at
CREATE OR REPLACE FUNCTION parent_schema.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_parent_profiles_updated_at
    BEFORE UPDATE ON parent_schema.parent_profiles
    FOR EACH ROW EXECUTE FUNCTION parent_schema.set_updated_at();
