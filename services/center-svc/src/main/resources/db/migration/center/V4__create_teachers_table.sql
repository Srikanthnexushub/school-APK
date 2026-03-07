-- V4__create_teachers_table.sql
CREATE TABLE center_schema.teachers (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    center_id    UUID         NOT NULL,
    user_id      UUID         NOT NULL,
    first_name   VARCHAR(100) NOT NULL,
    last_name    VARCHAR(100) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    subjects     VARCHAR(500),
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    joined_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ,
    version      BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_teachers          PRIMARY KEY (id),
    CONSTRAINT fk_teachers_center   FOREIGN KEY (center_id) REFERENCES center_schema.centers (id),
    CONSTRAINT chk_teachers_status  CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE UNIQUE INDEX idx_teachers_user_center
    ON center_schema.teachers (user_id, center_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_teachers_center_id ON center_schema.teachers (center_id) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_teachers_updated_at
    BEFORE UPDATE ON center_schema.teachers
    FOR EACH ROW EXECUTE FUNCTION center_schema.set_updated_at();
