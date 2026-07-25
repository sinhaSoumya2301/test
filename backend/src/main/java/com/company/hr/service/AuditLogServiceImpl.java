package com.company.hr.service;

import com.company.hr.model.entity.AuditLog;
import com.company.hr.model.entity.Employee;
import com.company.hr.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    // REQUIRES_NEW: an audit-write failure must never roll back the business transaction it's
    // describing, and a business rollback must not erase the fact that something was attempted.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String entityType, UUID entityId, String action, Employee performedBy,
                        Object oldValue, Object newValue) {
        AuditLog entry = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .performedBy(performedBy)
                .oldValue(writeJson(oldValue))
                .newValue(writeJson(newValue))
                .build();
        auditLogRepository.save(entry);
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit log value, storing null: {}", e.getMessage());
            return null;
        }
    }
}
