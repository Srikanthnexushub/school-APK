-- V14__create_job_postings_table.sql
-- Creates the job_postings table for center-managed job listings.
-- Supports public job board browsing (OPEN status) and private management by CENTER_ADMIN.

CREATE TABLE center_schema.job_postings (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    center_id             UUID         NOT NULL,
    title                 VARCHAR(200) NOT NULL,
    description           TEXT,
    role_type             VARCHAR(50)  NOT NULL,
    subjects              TEXT,                          -- comma-separated; nullable (teaching roles only)
    qualifications        TEXT,                          -- comma-separated; nullable
    experience_min_years  INTEGER,
    job_type              VARCHAR(30)  NOT NULL DEFAULT 'FULL_TIME',
    salary_min            INTEGER,
    salary_max            INTEGER,
    deadline              DATE,
    status                VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    posted_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at            TIMESTAMP WITH TIME ZONE
);

-- Efficient lookup of all jobs for a given center (admin management view)
CREATE INDEX idx_job_postings_center_id
    ON center_schema.job_postings (center_id);

-- Efficient public job board queries (OPEN, not soft-deleted)
CREATE INDEX idx_job_postings_status
    ON center_schema.job_postings (status)
    WHERE deleted_at IS NULL;

-- Efficient role-type filtering on the public job board
CREATE INDEX idx_job_postings_role_type
    ON center_schema.job_postings (role_type)
    WHERE deleted_at IS NULL AND status = 'OPEN';
