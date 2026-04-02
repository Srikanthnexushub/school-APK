CREATE TABLE IF NOT EXISTS curricular_schema.topic_prerequisites (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    from_topic_id UUID        NOT NULL REFERENCES curricular_schema.topics(id),
    to_topic_id   UUID        NOT NULL REFERENCES curricular_schema.topics(id),
    strength      VARCHAR(20) NOT NULL DEFAULT 'REQUIRED' CHECK (strength IN ('REQUIRED','RECOMMENDED')),
    created_by    UUID        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (from_topic_id, to_topic_id),
    CHECK (from_topic_id != to_topic_id)
);

CREATE INDEX IF NOT EXISTS idx_topic_prereqs_from ON curricular_schema.topic_prerequisites(from_topic_id);
CREATE INDEX IF NOT EXISTS idx_topic_prereqs_to ON curricular_schema.topic_prerequisites(to_topic_id);
