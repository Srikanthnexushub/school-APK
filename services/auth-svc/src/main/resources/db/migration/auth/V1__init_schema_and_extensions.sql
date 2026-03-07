-- V1__init_schema_and_extensions.sql
-- Ensure the auth_schema exists and extensions are available.
-- Extensions are already enabled at the DB level by init-db/01-create-schemas.sql,
-- but IF NOT EXISTS guards make this idempotent.

CREATE SCHEMA IF NOT EXISTS auth_schema;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
