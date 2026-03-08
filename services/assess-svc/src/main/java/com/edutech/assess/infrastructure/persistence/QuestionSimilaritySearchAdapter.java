// src/main/java/com/edutech/assess/infrastructure/persistence/QuestionSimilaritySearchAdapter.java
package com.edutech.assess.infrastructure.persistence;

import com.edutech.assess.domain.model.Question;
import com.edutech.assess.domain.port.out.QuestionSimilaritySearchPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implements similarity search against the pgvector-backed embedding column
 * in assess_schema.questions using cosine distance (<=> operator).
 *
 * pgvector wire format: "[f1,f2,...,fn]" cast to ::vector in SQL.
 * We build the literal ourselves to avoid pulling PGvector (which extends PGobject
 * from the runtime-scoped PostgreSQL JDBC driver) into compile-time dependencies.
 */
@Component
class QuestionSimilaritySearchAdapter implements QuestionSimilaritySearchPort {

    private final JdbcTemplate jdbcTemplate;

    QuestionSimilaritySearchAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Question> findSimilar(float[] queryEmbedding, int topK, List<UUID> excludeIds) {
        if (topK <= 0) {
            return Collections.emptyList();
        }

        String vectorLiteral = toVectorLiteral(queryEmbedding);

        if (excludeIds == null || excludeIds.isEmpty()) {
            String sql = """
                    SELECT id, exam_id, question_text, options_json, correct_answer,
                           explanation, marks, difficulty, discrimination, guessing_param,
                           version, created_at, updated_at, deleted_at
                      FROM assess_schema.questions
                     WHERE deleted_at IS NULL
                       AND embedding IS NOT NULL
                     ORDER BY embedding <=> ?::vector
                     LIMIT ?
                    """;
            return jdbcTemplate.query(sql, QUESTION_ROW_MAPPER, vectorLiteral, topK);
        }

        // Build IN-list placeholders — UUIDs are safe strings, no injection risk
        String placeholders = excludeIds.stream()
                .map(id -> "'" + id.toString() + "'::uuid")
                .collect(Collectors.joining(", "));

        String sql = String.format("""
                SELECT id, exam_id, question_text, options_json, correct_answer,
                       explanation, marks, difficulty, discrimination, guessing_param,
                       version, created_at, updated_at, deleted_at
                  FROM assess_schema.questions
                 WHERE deleted_at IS NULL
                   AND embedding IS NOT NULL
                   AND id NOT IN (%s)
                 ORDER BY embedding <=> ?::vector
                 LIMIT ?
                """, placeholders);

        return jdbcTemplate.query(sql, QUESTION_ROW_MAPPER, vectorLiteral, topK);
    }

    @Override
    public void saveEmbedding(UUID questionId, float[] embedding) {
        jdbcTemplate.update(
                "UPDATE assess_schema.questions SET embedding = ?::vector WHERE id = ?::uuid",
                toVectorLiteral(embedding),
                questionId.toString()
        );
    }

    /**
     * Convert a float array to pgvector wire literal format: "[f1,f2,...,fn]".
     */
    private static String toVectorLiteral(float[] floats) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < floats.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(floats[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private static final RowMapper<Question> QUESTION_ROW_MAPPER = (rs, rowNum) ->
            Question.reconstitute(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("exam_id")),
                    rs.getString("question_text"),
                    rs.getString("options_json"),
                    rs.getInt("correct_answer"),
                    rs.getString("explanation"),
                    rs.getDouble("marks"),
                    rs.getDouble("difficulty"),
                    rs.getDouble("discrimination"),
                    rs.getDouble("guessing_param"),
                    rs.getLong("version"),
                    rs.getObject("created_at", Instant.class),
                    rs.getObject("updated_at", Instant.class),
                    rs.getObject("deleted_at", Instant.class)
            );
}
