-- Fix: chapter_number was created as SMALLINT but Hibernate entity uses int (INTEGER)
-- Alter to INTEGER so Hibernate schema validation passes.
ALTER TABLE curricular_schema.chapters
    ALTER COLUMN chapter_number TYPE INTEGER USING chapter_number::INTEGER;
