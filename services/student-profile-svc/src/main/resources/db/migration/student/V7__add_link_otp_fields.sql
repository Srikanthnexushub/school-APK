ALTER TABLE student_schema.students ADD COLUMN IF NOT EXISTS link_otp VARCHAR(6);
ALTER TABLE student_schema.students ADD COLUMN IF NOT EXISTS link_otp_expires_at TIMESTAMPTZ;
ALTER TABLE student_schema.students ADD COLUMN IF NOT EXISTS link_otp_parent_user_id UUID;
ALTER TABLE student_schema.students ADD COLUMN IF NOT EXISTS link_otp_parent_name TEXT;
