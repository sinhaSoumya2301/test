package com.company.hr.exception;

import org.springframework.http.HttpStatus;

/**
 * Ownership-check failures — the caller is authenticated and has the right role, but is not
 * the right *actor* for this specific action (e.g. not the target of this swap request, or not
 * this requester's actual manager — SPEC.md "Threat Model" / FR-12). Distinct from Spring
 * Security's role-based AccessDeniedException, which this app maps via RestAccessDeniedHandler.
 */
public class UnauthorizedActionException extends ApiException {

    public UnauthorizedActionException(String code, String message) {
        super(HttpStatus.FORBIDDEN, code, message);
    }
}
