-- V1__init_schema.sql
-- Initialize parent_schema and required PostgreSQL extensions

CREATE SCHEMA IF NOT EXISTS parent_schema;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

SET search_path TO parent_schema;
