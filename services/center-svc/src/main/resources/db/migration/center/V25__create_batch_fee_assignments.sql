-- V25__create_batch_fee_assignments.sql
-- Links a fee structure to a batch with effective date range.
-- Allows a center to assign different fee structures to different batches.
CREATE TABLE center_schema.batch_fee_assignments (
    id               UUID        NOT NULL DEFAULT gen_random_uuid(),
    batch_id         UUID        NOT NULL,
    center_id        UUID        NOT NULL,
    fee_structure_id UUID        NOT NULL,
    effective_from   DATE        NOT NULL,
    effective_to     DATE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_batch_fee_assignments PRIMARY KEY (id),
    CONSTRAINT fk_bfa_fee_structure     FOREIGN KEY (fee_structure_id)
        REFERENCES center_schema.fee_structures(id),
    CONSTRAINT uq_batch_fee_active      UNIQUE (batch_id, fee_structure_id, effective_from)
);

CREATE INDEX idx_bfa_batch  ON center_schema.batch_fee_assignments(batch_id);
CREATE INDEX idx_bfa_center ON center_schema.batch_fee_assignments(center_id);
