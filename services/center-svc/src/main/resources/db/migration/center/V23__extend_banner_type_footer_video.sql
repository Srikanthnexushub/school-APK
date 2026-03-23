-- V23: Extend banner_type check constraint to include FOOTER_VIDEO type.
-- FOOTER_VIDEO banners render as a compact autoplay video strip in the footer area of dashboards.
-- Drops and recreates the constraint to allow HERO, TICKER, VIDEO, and FOOTER_VIDEO.

ALTER TABLE center_schema.banners
    DROP CONSTRAINT IF EXISTS chk_banner_type;

ALTER TABLE center_schema.banners
    ADD CONSTRAINT chk_banner_type CHECK (banner_type IN ('HERO', 'TICKER', 'VIDEO', 'FOOTER_VIDEO'));
