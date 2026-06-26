package com.finbridge.exception;

/** Thrown when authentication fails (e.g. bad credentials). Maps to HTTP 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
