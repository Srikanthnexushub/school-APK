-- V19: Extend banner_type check constraint to include VIDEO type.
-- Drops and recreates the constraint to allow HERO, TICKER, and VIDEO.

ALTER TABLE center_schema.banners
    DROP CONSTRAINT IF EXISTS chk_banner_type;

ALTER TABLE center_schema.banners
    ADD CONSTRAINT chk_banner_type CHECK (banner_type IN ('HERO', 'TICKER', 'VIDEO'));
