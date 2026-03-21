-- V18: Add video_url column to support VIDEO banner type
-- VIDEO banners render as autoplay muted video advertisements (5-second loop)
ALTER TABLE center_schema.banners ADD COLUMN IF NOT EXISTS video_url TEXT;
