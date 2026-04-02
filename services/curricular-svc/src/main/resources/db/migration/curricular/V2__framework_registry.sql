CREATE TABLE IF NOT EXISTS curricular_schema.curriculum_boards (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_code  VARCHAR(30)  NOT NULL UNIQUE,
    board_name  VARCHAR(120) NOT NULL,
    country_code CHAR(2)     NOT NULL DEFAULT 'IN',
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS curricular_schema.subjects (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id     UUID        NOT NULL REFERENCES curricular_schema.curriculum_boards(id),
    subject_code VARCHAR(30) NOT NULL,
    subject_name VARCHAR(120) NOT NULL,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ,
    UNIQUE (board_id, subject_code)
);

CREATE TABLE IF NOT EXISTS curricular_schema.grade_levels (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    board_id     UUID        NOT NULL REFERENCES curricular_schema.curriculum_boards(id),
    grade_code   VARCHAR(20) NOT NULL,
    display_name VARCHAR(60) NOT NULL,
    sort_order   SMALLINT    NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (board_id, grade_code)
);

CREATE TABLE IF NOT EXISTS curricular_schema.chapters (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subject_id       UUID           NOT NULL REFERENCES curricular_schema.subjects(id),
    grade_id         UUID           NOT NULL REFERENCES curricular_schema.grade_levels(id),
    chapter_number   SMALLINT       NOT NULL,
    chapter_title    VARCHAR(200)   NOT NULL,
    total_est_hours  NUMERIC(6,2)   NOT NULL DEFAULT 0,
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ,
    UNIQUE (subject_id, grade_id, chapter_number)
);

CREATE TABLE IF NOT EXISTS curricular_schema.topics (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    chapter_id   UUID        NOT NULL REFERENCES curricular_schema.chapters(id),
    topic_code   VARCHAR(40) NOT NULL UNIQUE,
    topic_title  VARCHAR(255) NOT NULL,
    blooms_level VARCHAR(20) NOT NULL CHECK (blooms_level IN ('REMEMBER','UNDERSTAND','APPLY','ANALYZE','EVALUATE','CREATE')),
    est_hours    NUMERIC(4,2) NOT NULL DEFAULT 1.0,
    sort_order   SMALLINT    NOT NULL DEFAULT 0,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_topics_chapter ON curricular_schema.topics(chapter_id);
CREATE INDEX IF NOT EXISTS idx_chapters_subject_grade ON curricular_schema.chapters(subject_id, grade_id);
