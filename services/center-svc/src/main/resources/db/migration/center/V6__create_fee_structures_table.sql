-- V6__create_fee_structures_table.sql
CREATE TABLE center_schema.fee_structures (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    center_id       UUID            NOT NULL,
    name            VARCHAR(200)    NOT NULL,
    description     VARCHAR(1000),
    amount          NUMERIC(12,2)   NOT NULL CHECK (amount > 0),
    currency        VARCHAR(5)      NOT NULL DEFAULT 'INR',
    frequency       VARCHAR(20)     NOT NULL,
    due_day         INT             NOT NULL CHECK (due_day BETWEEN 1 AND 31),
    late_fee_amount NUMERIC(12,2)   CHECK (late_fee_amount >= 0),
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version         BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_fee_structures        PRIMARY KEY (id),
    CONSTRAINT fk_fee_structures_center FOREIGN KEY (center_id) REFERENCES center_schema.centers (id),
    CONSTRAINT chk_fee_frequency        CHECK (frequency IN ('MONTHLY','QUARTERLY','ANNUAL','ONE_TIME')),
    CONSTRAINT chk_fee_status           CHECK (status IN ('ACTIVE','ARCHIVED'))
);

CREATE INDEX idx_fee_structures_center_id ON center_schema.fee_structures (center_id);

CREATE TRIGGER trg_fee_structures_updated_at
    BEFORE UPDATE ON center_schema.fee_structures
    FOR EACH ROW EXECUTE FUNCTION center_schema.set_updated_at();
