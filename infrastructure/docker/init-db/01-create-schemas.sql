-- =============================================================================
-- EduTech AI Platform — Database Initialisation Script
-- Runs once on first Postgres container startup.
-- Creates per-service users and schemas with strict isolation.
-- All credentials come from environment variables (no hardcoded values).
-- =============================================================================

-- Enable extensions (requires superuser — done here, once)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";       -- pgvector
CREATE EXTENSION IF NOT EXISTS "timescaledb";  -- TimescaleDB

-- =============================================================================
-- auth-svc
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = current_setting('app.auth_db_user')) THEN
        EXECUTE format('CREATE USER %I WITH PASSWORD %L',
            current_setting('app.auth_db_user'),
            current_setting('app.auth_db_password'));
    END IF;
END $$;

CREATE DATABASE auth_db OWNER = current_user;
\connect auth_db
CREATE SCHEMA IF NOT EXISTS auth_schema;
GRANT ALL PRIVILEGES ON SCHEMA auth_schema TO current_user;

-- =============================================================================
-- NOTE: Remaining databases (parent_db, center_db, assess_db, psych_db)
-- are created in subsequent init scripts (02-05) so Flyway migrations
-- handle schema creation within each service on startup.
-- This file only bootstraps extensions and the auth database.
-- =============================================================================
