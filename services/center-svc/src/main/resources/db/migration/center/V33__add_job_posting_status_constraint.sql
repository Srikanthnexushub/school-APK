-- Add CHECK constraint for JobPostingStatus enum values
ALTER TABLE center_schema.job_postings
    DROP CONSTRAINT IF EXISTS job_postings_status_check;

ALTER TABLE center_schema.job_postings
    ADD CONSTRAINT job_postings_status_check
    CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED', 'FILLED'));
