// src/main/java/com/edutech/assess/domain/port/out/QuestionSimilaritySearchPort.java
package com.edutech.assess.domain.port.out;

import com.edutech.assess.domain.model.Question;

import java.util.List;
import java.util.UUID;

/**
 * Port for finding semantically similar questions using pgvector cosine similarity.
 * Infrastructure adapter uses the pgvector JDBC extension.
 */
public interface QuestionSimilaritySearchPort {

    /**
     * Find top-K questions most similar to the given embedding vector.
     *
     * @param queryEmbedding float array of length 1536 (OpenAI text-embedding-3-small)
     * @param topK           max number of results
     * @param excludeIds     question IDs to exclude (e.g. already in current exam)
     * @return ordered list of similar questions, most similar first
     */
    List<Question> findSimilar(float[] queryEmbedding, int topK, List<UUID> excludeIds);

    /**
     * Persist embedding for a question after it has been generated via ai-gateway-svc.
     */
    void saveEmbedding(UUID questionId, float[] embedding);
}
