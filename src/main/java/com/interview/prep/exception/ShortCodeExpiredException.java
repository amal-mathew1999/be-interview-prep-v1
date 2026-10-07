package com.interview.prep.exception;

public class ShortCodeExpiredException extends RuntimeException {

    public ShortCodeExpiredException(String shortCode) {
        super("Short code has expired: " + shortCode);
    }
}
