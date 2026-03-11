ALTER TABLE parent_schema.student_links
    ALTER COLUMN standard TYPE TEXT USING standard::text;
