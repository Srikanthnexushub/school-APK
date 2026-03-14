-- V1: Initialize performance_schema and required extensions

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- TimescaleDB is optional — not available in all local dev environments
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'timescaledb extension not available — skipping (hypertables will not be created)';
END
$$;

CREATE SCHEMA IF NOT EXISTS performance_schema;
