package com.company.hr.exception;

import org.springframework.http.HttpStatus;

/** Login rate limiting (SPEC.md "Security & Vulnerability Scanning" / "Threat Model" DoS mitigation). */
public class TooManyRequestsException extends ApiException {

    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", message);
    }
}
