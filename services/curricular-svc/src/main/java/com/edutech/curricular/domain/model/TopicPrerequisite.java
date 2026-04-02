package com.edutech.curricular.domain.model;

import com.edutech.curricular.domain.model.enums.PrerequisiteStrength;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "topic_prerequisites")
@Getter
@Setter
@NoArgsConstructor
public class TopicPrerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "from_topic_id", nullable = false)
    private UUID fromTopicId;

    @Column(name = "to_topic_id", nullable = false)
    private UUID toTopicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "strength", nullable = false, length = 20)
    private PrerequisiteStrength strength = PrerequisiteStrength.REQUIRED;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static TopicPrerequisite create(UUID fromTopicId, UUID toTopicId, PrerequisiteStrength strength, UUID createdBy) {
        TopicPrerequisite tp = new TopicPrerequisite();
        tp.fromTopicId = fromTopicId;
        tp.toTopicId = toTopicId;
        tp.strength = strength != null ? strength : PrerequisiteStrength.REQUIRED;
        tp.createdBy = createdBy;
        tp.createdAt = Instant.now();
        return tp;
    }
}
