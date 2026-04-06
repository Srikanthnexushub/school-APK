// src/main/java/com/edutech/center/application/dto/CreateBatchRequest.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.BatchMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a batch.
 *
 * <p>Implemented as a class (not a record) so that the canonical 8-arg positional
 * constructor used by existing integration tests continues to compile unchanged,
 * while new code can supply the optional {@code grade} field via the 9-arg constructor.
 *
 * <p>Accessor methods follow the Java record naming convention (no "get" prefix) so
 * that all service/controller code calling {@code request.grade()} etc. compiles
 * identically for both old and new call sites.
 */
public final class CreateBatchRequest {

    @NotBlank @Size(max = 200)
    private final String name;

    @NotBlank @Size(max = 50)
    private final String code;

    @NotBlank @Size(max = 100)
    private final String subject;

    private final UUID teacherId;

    @Min(1) @Max(200)
    private final int maxStudents;

    @NotNull
    private final LocalDate startDate;

    private final LocalDate endDate;

    private final BatchMode mode;

    /** Grade/class for this batch. Nullable — VARCHAR(10) limit enforced at DB layer. */
    @Size(max = 10)
    private final String grade;

    // ------------------------------------------------------------------
    // Backward-compatible 8-arg constructor — preserves all existing IT call sites
    // ------------------------------------------------------------------
    public CreateBatchRequest(String name, String code, String subject,
                              UUID teacherId, int maxStudents,
                              LocalDate startDate, LocalDate endDate,
                              BatchMode mode) {
        this(name, code, subject, teacherId, maxStudents, startDate, endDate, mode, null);
    }

    // ------------------------------------------------------------------
    // Full 9-arg constructor with grade
    // ------------------------------------------------------------------
    public CreateBatchRequest(String name, String code, String subject,
                              UUID teacherId, int maxStudents,
                              LocalDate startDate, LocalDate endDate,
                              BatchMode mode, String grade) {
        this.name = name;
        this.code = code;
        this.subject = subject;
        this.teacherId = teacherId;
        this.maxStudents = maxStudents;
        this.startDate = startDate;
        this.endDate = endDate;
        this.mode = mode;
        this.grade = grade;
    }

    // ------------------------------------------------------------------
    // Record-style accessors (no "get" prefix — matches record convention)
    // ------------------------------------------------------------------
    public String name()        { return name; }
    public String code()        { return code; }
    public String subject()     { return subject; }
    public UUID teacherId()     { return teacherId; }
    public int maxStudents()    { return maxStudents; }
    public LocalDate startDate(){ return startDate; }
    public LocalDate endDate()  { return endDate; }
    public BatchMode mode()     { return mode; }
    public String grade()       { return grade; }
}
