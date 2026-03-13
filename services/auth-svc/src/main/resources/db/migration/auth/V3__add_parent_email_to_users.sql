ALTER TABLE auth_schema.users ADD COLUMN IF NOT EXISTS parent_email VARCHAR(255);
