package com.edutech.mentorsvc.application.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSessionStatusRequest(
    @NotNull(message = "status is required")
    String status
) {}
