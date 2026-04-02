package com.edutech.curricular.domain.model;

import com.edutech.curricular.domain.model.enums.BloomsLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "curricular_schema", name = "topics")
@Getter
@Setter
@NoArgsConstructor
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(name = "topic_code", unique = true, nullable = false, length = 40)
    private String topicCode;

    @Column(name = "topic_title", nullable = false, length = 255)
    private String topicTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "blooms_level", nullable = false, length = 20)
    private BloomsLevel bloomsLevel;

    @Column(name = "est_hours", precision = 4, scale = 2)
    private BigDecimal estHours = BigDecimal.ONE;

    @Column(name = "sort_order")
    private int sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public static Topic create(UUID chapterId, String topicCode, String topicTitle, BloomsLevel bloomsLevel, BigDecimal estHours, int sortOrder) {
        Topic t = new Topic();
        t.chapterId = chapterId;
        t.topicCode = topicCode;
        t.topicTitle = topicTitle;
        t.bloomsLevel = bloomsLevel;
        t.estHours = estHours != null ? estHours : BigDecimal.ONE;
        t.sortOrder = sortOrder;
        t.active = true;
        t.createdAt = Instant.now();
        return t;
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }
}
