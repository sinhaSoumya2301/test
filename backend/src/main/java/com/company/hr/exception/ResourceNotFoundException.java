package com.company.hr.exception;

import org.springframework.http.HttpStatus;

/**
 * Used for both "doesn't exist" and "exists but not visible to this caller" — the API
 * intentionally returns 404 for both to avoid leaking existence (SPEC.md "API Contract").
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}
