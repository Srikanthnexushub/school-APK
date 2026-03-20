// src/main/java/com/edutech/center/application/exception/JobPostingNotFoundException.java
package com.edutech.center.application.exception;

import java.util.UUID;

public class JobPostingNotFoundException extends CenterException {
    public JobPostingNotFoundException(UUID jobId) {
        super("Job posting not found: " + jobId);
    }
}
