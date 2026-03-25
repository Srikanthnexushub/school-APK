-- V15__add_fee_structure_id_to_payments.sql
-- Links a fee payment to the center-svc fee structure it covers.
-- Nullable so existing records are unaffected.
ALTER TABLE parent_schema.fee_payments
    ADD COLUMN fee_structure_id UUID;
CREATE INDEX idx_fee_payments_structure ON parent_schema.fee_payments(fee_structure_id)
    WHERE fee_structure_id IS NOT NULL;
