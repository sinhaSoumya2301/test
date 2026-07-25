package com.company.hr.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Deliberately holds no peer/manager decision fields — each decision is a separate Approval
 * row (see Approval.java) linked back to this request. "Who may decide the MANAGER stage" is
 * resolved dynamically from requester.getManager() rather than snapshotted here, so an org
 * hierarchy change is always reflected up to the moment a decision is actually made.
 *
 * The version column backs optimistic locking so a manager double-clicking "Approve" can only
 * ever have one of two concurrent decision calls win the write (see SPEC.md "Idempotency Testing").
 */
@Entity
@Table(name = "shift_swap_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftSwapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private Employee requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_shift_id", nullable = false)
    private Shift requesterShift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_employee_id", nullable = false)
    private Employee targetEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_shift_id")
    private Shift targetShift;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private SwapStatus status = SwapStatus.PENDING_PEER_APPROVAL;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
