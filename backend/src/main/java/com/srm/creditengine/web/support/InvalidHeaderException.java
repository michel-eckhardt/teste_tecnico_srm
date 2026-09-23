package com.srm.creditengine.web.support;

/** A request header is present but malformed (answered as a 400 {@code VALIDATION_ERROR}). */
public class InvalidHeaderException extends RuntimeException {

    private final String header;

    public InvalidHeaderException(String header, String message) {
        super(message);
        this.header = header;
    }

    public String header() {
        return header;
    }
}
