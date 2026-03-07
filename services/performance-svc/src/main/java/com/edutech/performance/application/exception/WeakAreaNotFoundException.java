package com.edutech.performance.application.exception;

import java.util.UUID;

public class WeakAreaNotFoundException extends PerformanceException {

    public WeakAreaNotFoundException(UUID id) {
        super("Weak area record not found with id=" + id);
    }
}
