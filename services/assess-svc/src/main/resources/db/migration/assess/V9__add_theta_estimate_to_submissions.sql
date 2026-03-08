-- V9__add_theta_estimate_to_submissions.sql
-- Adds IRT 3PL theta estimate column to submissions table.
-- theta_estimate is NULL until a submission is graded and IRT estimation succeeds.
-- Range: [-4.0, 4.0] following standard IRT convention.
ALTER TABLE assess_schema.submissions
    ADD COLUMN IF NOT EXISTS theta_estimate DOUBLE PRECISION;
