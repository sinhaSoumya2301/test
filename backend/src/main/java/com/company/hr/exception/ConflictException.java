package com.company.hr.exception;

import org.springframework.http.HttpStatus;

/**
 * Domain-data conflicts — e.g. the requester's or target's shift overlaps an existing
 * approved leave/another shift once the swap would take effect. Distinct from
 * InvalidTransitionException, which is reserved for illegal state-machine moves.
 */
public class ConflictException extends ApiException {

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
