package com.masjidapp.exception;

public class EventNotFoundException extends ResourceNotFoundException {
    public EventNotFoundException(String message) {
        super(message);
    }
}
