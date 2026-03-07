-- V1__init_schema.sql
CREATE SCHEMA IF NOT EXISTS assess_schema;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
-- vector extension for future question embeddings (pgvector)
-- CREATE EXTENSION IF NOT EXISTS vector;  -- Enable when pgvector is installed
SET search_path TO assess_schema;
