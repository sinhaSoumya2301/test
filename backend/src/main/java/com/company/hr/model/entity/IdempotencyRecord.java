package com.company.hr.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Backs the `Idempotency-Key` header contract in SPEC.md "API Contract": a retried request
 * with the same (key, endpoint, employee) replays the stored response instead of re-running
 * the side effect. Records older than 24h are eligible for the key to be reused (see
 * SPEC.md "Idempotency Testing") — expiry is enforced in the service layer, not here.
 */
@Entity
@Table(
        name = "idempotency_record",
        uniqueConstraints = @UniqueConstraint(columnNames = {"idempotency_key", "endpoint", "employee_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "endpoint", nullable = false, length = 100)
    private String endpoint;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", nullable = false, columnDefinition = "text")
    private String responseBody;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
