package com.edutech.parent.application.exception;

public class TooManyChildrenException extends RuntimeException {
    public TooManyChildrenException() {
        super("A parent can link a maximum of 5 children.");
    }
}
