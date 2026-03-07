-- V2__create_centers_table.sql
CREATE TABLE center_schema.centers (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    code        VARCHAR(20)  NOT NULL,
    address     VARCHAR(500) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    state       VARCHAR(100) NOT NULL,
    pincode     VARCHAR(10)  NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    email       VARCHAR(255) NOT NULL,
    website     VARCHAR(500),
    logo_url    VARCHAR(1000),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    owner_id    UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ,
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_centers        PRIMARY KEY (id),
    CONSTRAINT uq_centers_code   UNIQUE      (code),
    CONSTRAINT chk_centers_status CHECK (status IN ('ACTIVE','SUSPENDED','CLOSED'))
);

CREATE INDEX idx_centers_owner_id  ON center_schema.centers (owner_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_centers_status    ON center_schema.centers (status)   WHERE deleted_at IS NULL;
CREATE INDEX idx_centers_created_at_brin ON center_schema.centers USING BRIN (created_at);

ALTER TABLE center_schema.centers ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE tablename = 'centers' AND schemaname = 'center_schema' AND policyname = 'centers_service_access'
    ) THEN
        EXECUTE format(
            'CREATE POLICY centers_service_access ON center_schema.centers TO %I USING (true)',
            current_user
        );
    END IF;
END $$;

CREATE OR REPLACE FUNCTION center_schema.set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$;

CREATE TRIGGER trg_centers_updated_at
    BEFORE UPDATE ON center_schema.centers
    FOR EACH ROW EXECUTE FUNCTION center_schema.set_updated_at();
