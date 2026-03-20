-- V17: Add banner_type to support HERO carousel vs TICKER (running marquee) banners.
-- Existing banners default to HERO (no data migration needed).

ALTER TABLE center_schema.banners
    ADD COLUMN IF NOT EXISTS banner_type TEXT NOT NULL DEFAULT 'HERO';

ALTER TABLE center_schema.banners
    ADD CONSTRAINT chk_banner_type CHECK (banner_type IN ('HERO', 'TICKER'));

CREATE INDEX IF NOT EXISTS idx_banners_type
    ON center_schema.banners (banner_type)
    WHERE deleted_at IS NULL AND is_active = TRUE;
