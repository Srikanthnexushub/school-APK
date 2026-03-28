-- Add plan_type, board, start_date, end_date to study_plans for Academic Plan feature
ALTER TABLE aimentor_schema.study_plans
    ADD COLUMN plan_type VARCHAR(30) NOT NULL DEFAULT 'CURRICULUM_PLAN',
    ADD COLUMN board VARCHAR(50),
    ADD COLUMN start_date DATE,
    ADD COLUMN end_date DATE;
