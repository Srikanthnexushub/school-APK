package com.edutech.curricular.domain.model;

import com.edutech.curricular.domain.model.enums.PathItemStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "learning_path_items")
@Getter
@Setter
@NoArgsConstructor
public class LearningPathItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "path_id", nullable = false)
    private UUID pathId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Column(name = "urgency_score", precision = 5, scale = 2)
    private BigDecimal urgencyScore = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PathItemStatus status = PathItemStatus.PENDING;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static LearningPathItem create(UUID pathId, UUID topicId, int sequenceOrder, BigDecimal urgencyScore) {
        LearningPathItem item = new LearningPathItem();
        item.pathId = pathId;
        item.topicId = topicId;
        item.sequenceOrder = sequenceOrder;
        item.urgencyScore = urgencyScore != null ? urgencyScore : BigDecimal.ZERO;
        item.status = PathItemStatus.PENDING;
        return item;
    }

    public void markInProgress() {
        this.status = PathItemStatus.IN_PROGRESS;
    }

    public void markCompleted() {
        this.status = PathItemStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void skip() {
        this.status = PathItemStatus.SKIPPED;
    }
}
