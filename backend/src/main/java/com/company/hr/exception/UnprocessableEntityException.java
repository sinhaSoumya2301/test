package com.company.hr.exception;

import org.springframework.http.HttpStatus;

/** Semantically invalid requests, e.g. swapping with yourself, a past-dated shift (SPEC.md 422 convention). */
public class UnprocessableEntityException extends ApiException {

    public UnprocessableEntityException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
