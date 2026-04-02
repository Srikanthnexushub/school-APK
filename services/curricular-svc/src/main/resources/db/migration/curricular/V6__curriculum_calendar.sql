CREATE TABLE IF NOT EXISTS curricular_schema.academic_years (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    center_id    UUID        NOT NULL,
    board_id     UUID        REFERENCES curricular_schema.curriculum_boards(id),
    grade_id     UUID        REFERENCES curricular_schema.grade_levels(id),
    year_label   VARCHAR(20) NOT NULL,
    start_date   DATE        NOT NULL,
    end_date     DATE        NOT NULL,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (center_id, board_id, grade_id, year_label)
);

CREATE TABLE IF NOT EXISTS curricular_schema.academic_terms (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    academic_year_id UUID        NOT NULL REFERENCES curricular_schema.academic_years(id) ON DELETE CASCADE,
    term_name        VARCHAR(60) NOT NULL,
    start_date       DATE        NOT NULL,
    end_date         DATE        NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_academic_years_center ON curricular_schema.academic_years(center_id);
CREATE INDEX IF NOT EXISTS idx_academic_terms_year ON curricular_schema.academic_terms(academic_year_id);
