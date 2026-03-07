CREATE SCHEMA IF NOT EXISTS psych_schema;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
-- pgvector for trait embedding similarity search
-- CREATE EXTENSION IF NOT EXISTS vector;  -- Enable when pgvector is installed on target DB
SET search_path TO psych_schema;
