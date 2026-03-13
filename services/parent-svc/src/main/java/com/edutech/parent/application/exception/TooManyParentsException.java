package com.edutech.parent.application.exception;

public class TooManyParentsException extends RuntimeException {
    public TooManyParentsException() {
        super("A student can be linked to a maximum of 2 parents/guardians.");
    }
}
