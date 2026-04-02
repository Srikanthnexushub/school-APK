-- Platform-wide resources: centerId nullable, is_platform_resource flag, stream filter
ALTER TABLE center_schema.content_items ALTER COLUMN center_id DROP NOT NULL;

ALTER TABLE center_schema.content_items
    ADD COLUMN IF NOT EXISTS is_platform_resource BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS stream               VARCHAR(30);

-- Composite index for profile-based discovery queries
CREATE INDEX IF NOT EXISTS idx_content_platform_profile
    ON center_schema.content_items (is_platform_resource, board, class_grade, stream)
    WHERE deleted_at IS NULL;

-- Index for center-scoped queries (still dominant path)
CREATE INDEX IF NOT EXISTS idx_content_center_status
    ON center_schema.content_items (center_id, status)
    WHERE deleted_at IS NULL;
