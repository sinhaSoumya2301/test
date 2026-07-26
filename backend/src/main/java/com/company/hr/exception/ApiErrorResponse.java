package com.company.hr.exception;

/** Matches the error envelope in /SPEC.md "API Contract" > Conventions exactly. */
public record ApiErrorResponse(ApiError error) {

    public record ApiError(String code, String message, String traceId) {
    }

    public static ApiErrorResponse of(String code, String message, String traceId) {
        return new ApiErrorResponse(new ApiError(code, message, traceId));
    }
}
