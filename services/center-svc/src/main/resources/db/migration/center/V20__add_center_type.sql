-- V20: Add center_type column to centers
-- Distinguishes Coaching Centers from Schools and Colleges
ALTER TABLE center_schema.centers
    ADD COLUMN IF NOT EXISTS center_type VARCHAR(30) NOT NULL DEFAULT 'COACHING_CENTER';

ALTER TABLE center_schema.centers
    ADD CONSTRAINT chk_center_type CHECK (center_type IN ('COACHING_CENTER', 'SCHOOL', 'COLLEGE'));
