package com.masjidapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class MARequestException extends RuntimeException {

    public MARequestException(String message) {
        super(message);
    }

    public MARequestException(String message, Throwable cause) {
        super(message, cause);
    }

}
