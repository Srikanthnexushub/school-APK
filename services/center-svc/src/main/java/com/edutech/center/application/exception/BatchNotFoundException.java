// src/main/java/com/edutech/center/application/exception/BatchNotFoundException.java
package com.edutech.center.application.exception;

import java.util.UUID;

public class BatchNotFoundException extends CenterException {
    public BatchNotFoundException(UUID batchId) {
        super("Batch not found: " + batchId);
    }
}
