// src/main/java/com/edutech/center/domain/model/Schedule.java
package com.edutech.center.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Weekly recurring schedule slot for a batch.
 * effectiveTo=null means the schedule is open-ended.
 * Conflict detection is done at the service layer before persisting.
 */
@Entity
@Table(name = "schedules", schema = "center_schema")
public class Schedule {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "batch_id", updatable = false, nullable = false)
    private UUID batchId;

    @Column(name = "center_id", updatable = false, nullable = false)
    private UUID centerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(length = 100)
    private String room;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected Schedule() {}

    private Schedule(UUID id, UUID batchId, UUID centerId, DayOfWeek dayOfWeek,
                     LocalTime startTime, LocalTime endTime, String room,
                     LocalDate effectiveFrom, LocalDate effectiveTo) {
        this.id = id;
        this.batchId = batchId;
        this.centerId = centerId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static Schedule create(UUID batchId, UUID centerId, DayOfWeek dayOfWeek,
                                  LocalTime startTime, LocalTime endTime, String room,
                                  LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        return new Schedule(UUID.randomUUID(), batchId, centerId, dayOfWeek,
                startTime, endTime, room, effectiveFrom, effectiveTo);
    }

    public void update(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                       String room, LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.updatedAt = Instant.now();
    }

    public boolean overlapsWith(Schedule other) {
        if (this.dayOfWeek != other.dayOfWeek) return false;
        if (!this.room.equalsIgnoreCase(other.room)) return false;
        return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
    }

    public UUID getId() { return id; }
    public UUID getBatchId() { return batchId; }
    public UUID getCenterId() { return centerId; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getRoom() { return room; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
