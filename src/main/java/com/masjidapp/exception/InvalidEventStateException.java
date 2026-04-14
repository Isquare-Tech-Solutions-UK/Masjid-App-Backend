package com.masjidapp.exception;

public class InvalidEventStateException extends RuntimeException {
    public InvalidEventStateException(String message) {
        super(message);
    }
}
