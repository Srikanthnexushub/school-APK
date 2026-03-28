-- Add chapter-level fields to study_plan_items for Curriculum Plan row-by-row scheduling
ALTER TABLE aimentor_schema.study_plan_items
    ADD COLUMN chapter_number      INTEGER,
    ADD COLUMN chapter_start_date  DATE,
    ADD COLUMN chapter_end_date    DATE;
