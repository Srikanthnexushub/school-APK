-- Reference/master data for psychometric trait dimensions
CREATE TABLE psych_schema.trait_dimensions (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code        TEXT         NOT NULL UNIQUE,
    name        TEXT         NOT NULL,
    category    TEXT         NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_td_category CHECK (category IN ('BIG_FIVE', 'RIASEC'))
);

-- Seed the Big Five dimensions
INSERT INTO psych_schema.trait_dimensions (code, name, category, description) VALUES
    ('OPENNESS',           'Openness to Experience', 'BIG_FIVE', 'Creativity, curiosity, and openness to new ideas'),
    ('CONSCIENTIOUSNESS',  'Conscientiousness',       'BIG_FIVE', 'Organization, dependability, and self-discipline'),
    ('EXTRAVERSION',       'Extraversion',            'BIG_FIVE', 'Sociability, assertiveness, and positive emotionality'),
    ('AGREEABLENESS',      'Agreeableness',           'BIG_FIVE', 'Cooperation, trust, and empathy'),
    ('NEUROTICISM',        'Neuroticism',             'BIG_FIVE', 'Emotional instability and negative emotionality'),
    ('RIASEC_R',           'Realistic',               'RIASEC',   'Preference for hands-on, practical work'),
    ('RIASEC_I',           'Investigative',           'RIASEC',   'Analytical and intellectual curiosity'),
    ('RIASEC_A',           'Artistic',                'RIASEC',   'Creative and expressive tendencies'),
    ('RIASEC_S',           'Social',                  'RIASEC',   'Helping and working with people'),
    ('RIASEC_E',           'Enterprising',            'RIASEC',   'Leadership and persuasion orientation'),
    ('RIASEC_C',           'Conventional',            'RIASEC',   'Preference for structured, orderly work');
