-- V21__expand_content_items.sql
-- Expands content_items table for the intelligent document library feature.
-- Drops and recreates chk_content_type to include new types (same pattern as V19 for banners).
-- Adds metadata columns: subject, board, class_grade, year_of_paper, exam_type, difficulty,
-- language, tags, download_count, ai_summary, thumbnail_url, mime_type, page_count,
-- minio_object_key, bucket_name.

-- 1. Drop the existing type check constraint so new ContentType values can be inserted.
ALTER TABLE center_schema.content_items
    DROP CONSTRAINT IF EXISTS chk_content_type;

-- 2. Recreate with all valid ContentType values (old + new).
ALTER TABLE center_schema.content_items
    ADD CONSTRAINT chk_content_type
        CHECK (type IN (
            'VIDEO',
            'PDF',
            'DOCUMENT',
            'QUIZ_REF',
            'LINK',
            'PREVIOUS_YEAR_PAPER',
            'NOTES',
            'FORMULA_SHEET',
            'MIND_MAP'
        ));

-- 3. Add new metadata columns (all nullable to preserve backward compatibility).
ALTER TABLE center_schema.content_items
    ADD COLUMN IF NOT EXISTS subject           VARCHAR(100),
    ADD COLUMN IF NOT EXISTS board             VARCHAR(50),
    ADD COLUMN IF NOT EXISTS class_grade       VARCHAR(20),
    ADD COLUMN IF NOT EXISTS year_of_paper     SMALLINT,
    ADD COLUMN IF NOT EXISTS exam_type         VARCHAR(30),
    ADD COLUMN IF NOT EXISTS difficulty        VARCHAR(10),
    ADD COLUMN IF NOT EXISTS language          VARCHAR(30) NOT NULL DEFAULT 'ENGLISH',
    ADD COLUMN IF NOT EXISTS tags              TEXT[],
    ADD COLUMN IF NOT EXISTS download_count    BIGINT      NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS ai_summary        TEXT,
    ADD COLUMN IF NOT EXISTS thumbnail_url     VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS mime_type         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS page_count        INT,
    ADD COLUMN IF NOT EXISTS minio_object_key  VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS bucket_name       VARCHAR(200);

-- 4. Add check constraints on new enums.
ALTER TABLE center_schema.content_items
    ADD CONSTRAINT chk_content_exam_type
        CHECK (exam_type IS NULL OR exam_type IN (
            'BOARD_EXAM','JEE','NEET','UPSC','GATE','UNIT_TEST','MOCK','PRACTICE'
        ));

ALTER TABLE center_schema.content_items
    ADD CONSTRAINT chk_content_difficulty
        CHECK (difficulty IS NULL OR difficulty IN ('EASY','MEDIUM','HARD'));

-- 5. Supporting indexes.
CREATE INDEX IF NOT EXISTS idx_content_subject
    ON center_schema.content_items (subject) WHERE deleted_at IS NULL AND subject IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_content_board_year
    ON center_schema.content_items (board, year_of_paper) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_content_exam_type
    ON center_schema.content_items (exam_type) WHERE deleted_at IS NULL AND exam_type IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_content_download_count
    ON center_schema.content_items (download_count DESC) WHERE deleted_at IS NULL;
