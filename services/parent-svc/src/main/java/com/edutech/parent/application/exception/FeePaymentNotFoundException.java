// src/main/java/com/edutech/parent/application/exception/FeePaymentNotFoundException.java
package com.edutech.parent.application.exception;

import java.util.UUID;

public class FeePaymentNotFoundException extends ParentException {
    public FeePaymentNotFoundException(UUID id) {
        super("Fee payment not found: " + id);
    }
}
