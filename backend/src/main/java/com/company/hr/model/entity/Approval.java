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
 * One row per decision in the two-stage swap workflow (docs/adr/0002-peer-accept-then-manager-approval.md):
 * a PEER-stage row records the target colleague's accept/decline, a MANAGER-stage row records
 * the requester's manager's approve/reject. At most one row per (request, stage) — enforced by
 * a unique constraint — so a decision can never be silently overwritten.
 */
@Entity
@Table(
        name = "approval",
        uniqueConstraints = @UniqueConstraint(columnNames = {"shift_swap_request_id", "stage"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_swap_request_id", nullable = false)
    private ShiftSwapRequest shiftSwapRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 20)
    private ApprovalStage stage;

    /** The target employee for a PEER-stage row; the requester's manager for a MANAGER-stage row. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_id", nullable = false)
    private Employee approver;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private ApprovalDecision decision;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
