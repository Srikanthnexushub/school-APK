-- V2__create_users_table.sql
-- Users table — the aggregate root for the auth domain.
-- All temporal fields are TIMESTAMPTZ (timezone-aware).
-- Soft delete via deleted_at — no physical DELETE statements permitted.

CREATE TABLE auth_schema.users (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL,
    status        VARCHAR(50)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    center_id     UUID,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    phone_number  VARCHAR(20),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ,
    version       BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_users         PRIMARY KEY (id),
    CONSTRAINT uq_users_email   UNIQUE      (email),
    CONSTRAINT chk_users_role   CHECK       (role IN (
        'SUPER_ADMIN','CENTER_ADMIN','TEACHER','PARENT','STUDENT','GUEST'
    )),
    CONSTRAINT chk_users_status CHECK       (status IN (
        'PENDING_VERIFICATION','ACTIVE','LOCKED','DEACTIVATED'
    ))
);

-- ── Indexes ────────────────────────────────────────────────────────────────
-- Partial index: only active (non-deleted) users for email lookups
CREATE UNIQUE INDEX idx_users_email_active
    ON auth_schema.users (email)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_users_role
    ON auth_schema.users (role)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_users_center_id
    ON auth_schema.users (center_id)
    WHERE center_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_users_status
    ON auth_schema.users (status)
    WHERE deleted_at IS NULL;

-- BRIN index for time-range queries on created_at (append-heavy table)
CREATE INDEX idx_users_created_at_brin
    ON auth_schema.users USING BRIN (created_at);

-- ── Row Level Security ─────────────────────────────────────────────────────
-- RLS is enabled but policies are defined per-environment via service user.
-- The service connects as a non-superuser — policies control row visibility.
ALTER TABLE auth_schema.users ENABLE ROW LEVEL SECURITY;

-- Policy: service user can see all rows (service-level multi-tenancy handled in app)
-- Production: replace 'auth_svc_user' with the actual DB username from ${AUTH_SVC_DB_USERNAME}
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE tablename = 'users' AND schemaname = 'auth_schema' AND policyname = 'users_service_access'
    ) THEN
        EXECUTE format(
            'CREATE POLICY users_service_access ON auth_schema.users TO %I USING (true)',
            current_user
        );
    END IF;
END $$;

-- ── Auto-update updated_at ─────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION auth_schema.set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON auth_schema.users
    FOR EACH ROW EXECUTE FUNCTION auth_schema.set_updated_at();
