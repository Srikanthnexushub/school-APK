-- V22__content_fts_index.sql
-- Adds a PostgreSQL full-text search (FTS) generated column and GIN index to content_items.
-- Uses STORED generated column so the tsvector is computed once on write, not on every read.
-- Covers: title, description, subject, ai_summary (coalesce to empty string for nulls).

ALTER TABLE center_schema.content_items
    ADD COLUMN IF NOT EXISTS fts_vector tsvector
        GENERATED ALWAYS AS (
            to_tsvector(
                'english',
                coalesce(title, '')       || ' ' ||
                coalesce(description, '') || ' ' ||
                coalesce(subject, '')     || ' ' ||
                coalesce(ai_summary, '')
            )
        ) STORED;

CREATE INDEX IF NOT EXISTS idx_content_fts
    ON center_schema.content_items USING GIN (fts_vector)
    WHERE deleted_at IS NULL;
