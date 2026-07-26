package com.company.hr.service;

import com.company.hr.model.entity.Employee;

import java.util.UUID;

/**
 * Writes to the immutable audit_log table (SPEC.md "Monitoring & Logging" / "Threat Model" —
 * repudiation). performedBy is nullable to allow system-initiated actions (e.g. a scheduled job).
 */
public interface AuditLogService {

    void record(String entityType, UUID entityId, String action, Employee performedBy,
                Object oldValue, Object newValue);
}
