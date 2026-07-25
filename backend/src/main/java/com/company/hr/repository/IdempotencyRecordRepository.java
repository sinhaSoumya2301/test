package com.company.hr.repository;

import com.company.hr.model.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByIdempotencyKeyAndEndpointAndEmployeeId(
            String idempotencyKey, String endpoint, UUID employeeId);
}
