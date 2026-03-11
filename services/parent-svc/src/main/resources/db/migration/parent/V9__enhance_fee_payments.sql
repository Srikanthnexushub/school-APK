ALTER TABLE parent_schema.fee_payments
    ADD COLUMN IF NOT EXISTS fee_type TEXT DEFAULT 'TUITION',
    ADD COLUMN IF NOT EXISTS payment_method TEXT DEFAULT 'CASH';
