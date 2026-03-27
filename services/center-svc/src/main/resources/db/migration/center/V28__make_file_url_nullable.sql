-- V28__make_file_url_nullable.sql
-- The MinIO document-library flow stores references in minio_object_key (added V21),
-- not file_url. Drop the NOT NULL constraint so confirmUpload() can persist items
-- without a file_url (the column is retained for backward compatibility).
ALTER TABLE center_schema.content_items ALTER COLUMN file_url DROP NOT NULL;
