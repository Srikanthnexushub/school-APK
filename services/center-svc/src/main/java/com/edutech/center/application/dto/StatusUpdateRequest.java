// src/main/java/com/edutech/center/application/dto/StatusUpdateRequest.java
package com.edutech.center.application.dto;

import com.edutech.center.domain.model.JobPostingStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for an explicit status transition on a job posting.
 * The service maps the requested status to the appropriate domain method
 * (publish / close / markFilled / toDraft).
 */
public record StatusUpdateRequest(

    @NotNull
    JobPostingStatus status

) {}
