// src/main/java/com/edutech/assess/application/dto/SimilarQuestionRequest.java
package com.edutech.assess.application.dto;

import java.util.List;
import java.util.UUID;

public record SimilarQuestionRequest(float[] queryEmbedding, int topK, List<UUID> excludeIds) {}
