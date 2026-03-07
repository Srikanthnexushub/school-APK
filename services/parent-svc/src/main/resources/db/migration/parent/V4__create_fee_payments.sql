-- V4__create_fee_payments.sql
-- Fee payment records. Parent-submitted record of payment made.

CREATE TABLE parent_schema.fee_payments (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id        UUID         NOT NULL REFERENCES parent_schema.parent_profiles(id),
    student_id       UUID         NOT NULL,
    center_id        UUID         NOT NULL,
    batch_id         UUID,
    amount_paid      NUMERIC(12,2) NOT NULL CHECK (amount_paid > 0),
    currency         TEXT         NOT NULL DEFAULT 'INR',
    payment_date     DATE         NOT NULL,
    reference_number TEXT         NOT NULL,
    remarks          TEXT,
    status           TEXT         NOT NULL DEFAULT 'PENDING',
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'CONFIRMED', 'DISPUTED', 'REFUNDED'))
);

-- Reference number should be unique per parent (one parent can't submit same receipt twice)
CREATE UNIQUE INDEX uq_fee_payments_reference
    ON parent_schema.fee_payments(parent_id, reference_number);

-- Lookup indexes
CREATE INDEX idx_fee_payments_parent_id
    ON parent_schema.fee_payments(parent_id, payment_date DESC);

CREATE INDEX idx_fee_payments_student_id
    ON parent_schema.fee_payments(parent_id, student_id);

-- BRIN for time-ordered audit queries
CREATE INDEX idx_fee_payments_created_brin
    ON parent_schema.fee_payments USING BRIN(created_at);

CREATE TRIGGER trg_fee_payments_updated_at
    BEFORE UPDATE ON parent_schema.fee_payments
    FOR EACH ROW EXECUTE FUNCTION parent_schema.set_updated_at();
