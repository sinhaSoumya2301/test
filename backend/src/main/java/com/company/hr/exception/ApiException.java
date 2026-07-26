package com.company.hr.exception;

import org.springframework.http.HttpStatus;

/**
 * Base for all application-level exceptions that should be surfaced to the client as the
 * standard error envelope (see /SPEC.md "API Contract" > Conventions). Never carries a raw
 * stack trace or internal detail in its message — messages here are client-safe by construction.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
