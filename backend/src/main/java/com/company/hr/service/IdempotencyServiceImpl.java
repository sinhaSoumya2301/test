package com.company.hr.service;

import com.company.hr.model.entity.Employee;
import com.company.hr.model.entity.IdempotencyRecord;
import com.company.hr.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyServiceImpl.class);

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${app.idempotency.record-ttl-hours}")
    private long ttlHours;

    public IdempotencyServiceImpl(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public <T> T execute(String idempotencyKey, String endpoint, Employee employee,
                          Class<T> responseType, Supplier<T> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }

        Optional<IdempotencyRecord> existing =
                repository.findByIdempotencyKeyAndEndpointAndEmployeeId(idempotencyKey, endpoint, employee.getId());
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (record.getCreatedAt().isAfter(Instant.now().minusSeconds(ttlHours * 3600))) {
                return deserialize(record.getResponseBody(), responseType);
            }
            // Expired — the key becomes reusable (SPEC.md "Idempotency Testing").
            repository.delete(record);
        }

        T result = action.get();
        try {
            saveRecord(idempotencyKey, endpoint, employee, result);
        } catch (DataIntegrityViolationException e) {
            // Lost a race against a concurrent identical request that inserted first — that
            // request's persisted side effect is authoritative, not this one's in-memory result.
            log.debug("Concurrent idempotent request for key={} endpoint={}, replaying the winner",
                    idempotencyKey, endpoint);
            return repository.findByIdempotencyKeyAndEndpointAndEmployeeId(idempotencyKey, endpoint, employee.getId())
                    .map(r -> deserialize(r.getResponseBody(), responseType))
                    .orElse(result);
        }
        return result;
    }

    private void saveRecord(String idempotencyKey, String endpoint, Employee employee, Object result) {
        String body = writeJson(result);
        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .endpoint(endpoint)
                .employee(employee)
                .responseStatus(200)
                .responseBody(body)
                .build();
        repository.save(record);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize idempotent response", e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize idempotent response", e);
        }
    }
}
