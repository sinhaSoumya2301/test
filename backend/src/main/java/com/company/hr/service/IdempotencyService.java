package com.company.hr.service;

import com.company.hr.model.entity.Employee;

import java.util.function.Supplier;

/**
 * Backs the `Idempotency-Key` header contract in SPEC.md "API Contract" > Conventions: a
 * retried request with the same (key, endpoint, employee) replays the original response
 * instead of re-running {@code action}. If idempotencyKey is null/blank, idempotency is
 * skipped entirely and action always runs (the header is optional per the spec).
 */
public interface IdempotencyService {

    <T> T execute(String idempotencyKey, String endpoint, Employee employee,
                   Class<T> responseType, Supplier<T> action);
}
