-- V8__activate_pgvector.sql
-- Enable pgvector extension in the assess_schema search path
CREATE EXTENSION IF NOT EXISTS vector SCHEMA assess_schema;

-- Drop the TEXT placeholder column added in V3
ALTER TABLE assess_schema.questions DROP COLUMN IF EXISTS embedding_json;

-- Add real pgvector column (1536 dims = OpenAI text-embedding-3-small)
-- NULL until embedding is generated asynchronously via ai-gateway-svc
ALTER TABLE assess_schema.questions ADD COLUMN embedding vector(1536);

-- IVFFlat approximate nearest-neighbor index for cosine similarity.
-- lists=100 is appropriate for up to ~1M rows.
-- NOTE: IVFFlat requires training data; the index is most effective after
-- at least (lists * 30) rows have non-NULL embeddings. Until then, queries
-- will fall back to a sequential scan, which is correct but slower.
CREATE INDEX idx_questions_embedding_ivfflat
    ON assess_schema.questions
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

COMMENT ON COLUMN assess_schema.questions.embedding IS
    'OpenAI text-embedding-3-small vector (1536 dims). NULL until embedding is generated via ai-gateway-svc.';
