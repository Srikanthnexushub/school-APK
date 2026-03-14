-- V8__activate_pgvector.sql
-- Enable pgvector extension — optional, not available in all local dev environments

DO $$
BEGIN
    -- Try to install pgvector; skip gracefully if not installed on this PostgreSQL instance
    CREATE EXTENSION IF NOT EXISTS vector SCHEMA assess_schema;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'pgvector extension not available — skipping vector column and index (semantic search disabled)';
    RETURN;
END
$$;

-- Only proceed with vector column + index if extension was successfully installed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        -- Drop the TEXT placeholder column added in V3
        ALTER TABLE assess_schema.questions DROP COLUMN IF EXISTS embedding_json;

        -- Add real pgvector column (1536 dims = OpenAI text-embedding-3-small)
        -- NULL until embedding is generated asynchronously via ai-gateway-svc
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'assess_schema'
              AND table_name   = 'questions'
              AND column_name  = 'embedding'
        ) THEN
            ALTER TABLE assess_schema.questions ADD COLUMN embedding vector(1536);
        END IF;

        -- IVFFlat approximate nearest-neighbor index for cosine similarity
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE schemaname = 'assess_schema'
              AND indexname   = 'idx_questions_embedding_ivfflat'
        ) THEN
            CREATE INDEX idx_questions_embedding_ivfflat
                ON assess_schema.questions
                USING ivfflat (embedding vector_cosine_ops)
                WITH (lists = 100);
        END IF;
    ELSE
        RAISE NOTICE 'pgvector not installed — vector column and index skipped';
    END IF;
END
$$;
