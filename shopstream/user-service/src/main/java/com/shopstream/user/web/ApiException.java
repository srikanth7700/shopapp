package com.shopstream.user.web;

import org.springframework.http.HttpStatus;

/** Throw this from anywhere to return a specific HTTP status with a message. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
