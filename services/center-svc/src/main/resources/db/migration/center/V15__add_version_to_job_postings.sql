-- V15__add_version_to_job_postings.sql
-- Adds the optimistic-locking version column missing from the initial V14 migration.

ALTER TABLE center_schema.job_postings
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
