-- V16__create_banners.sql
-- Platform-level promotional banners managed by SUPER_ADMIN.
-- Displayed on Parent and Institution (CENTER_ADMIN) dashboards.

CREATE TABLE center_schema.banners (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title            TEXT         NOT NULL,
    subtitle         TEXT,
    image_url        TEXT,
    link_url         TEXT,
    link_label       TEXT,
    audience         TEXT         NOT NULL DEFAULT 'ALL',
    bg_color         TEXT,
    display_order    INT          NOT NULL DEFAULT 0,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    start_date       TIMESTAMPTZ,
    end_date         TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ,
    CONSTRAINT chk_banner_audience CHECK (audience IN ('PARENT','CENTER_ADMIN','ALL'))
);

CREATE INDEX idx_banners_audience ON center_schema.banners (audience) WHERE deleted_at IS NULL AND is_active = TRUE;
CREATE INDEX idx_banners_order    ON center_schema.banners (display_order) WHERE deleted_at IS NULL AND is_active = TRUE;
