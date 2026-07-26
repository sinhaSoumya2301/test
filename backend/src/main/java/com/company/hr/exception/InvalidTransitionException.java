package com.company.hr.exception;

import org.springframework.http.HttpStatus;

/**
 * An illegal move against the ShiftSwapRequest.status state machine — e.g. deciding a request
 * that's already terminal, or a duplicate decision at a stage that already has an Approval row.
 * Distinct from ConflictException, which is reserved for overlapping shift/leave conflicts.
 */
public class InvalidTransitionException extends ApiException {

    public InvalidTransitionException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
