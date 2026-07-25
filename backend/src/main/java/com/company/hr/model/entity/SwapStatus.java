package com.company.hr.model.entity;

/**
 * See SPEC.md "ShiftSwapRequest.status state machine":
 * PENDING_PEER_APPROVAL -accept-> PENDING_MANAGER_APPROVAL -approve-> APPROVED
 *                        -decline-> REJECTED_BY_PEER
 *                                                          -reject-> REJECTED_BY_MANAGER
 * (any PENDING_*) -cancel (requester)-> CANCELLED
 * (any PENDING_*) -7 days elapsed-> EXPIRED
 *
 * The decision that drives each transition is recorded as a separate Approval row
 * (see Approval.java) rather than as columns on this entity.
 */
public enum SwapStatus {
    PENDING_PEER_APPROVAL,
    PENDING_MANAGER_APPROVAL,
    REJECTED_BY_PEER,
    REJECTED_BY_MANAGER,
    APPROVED,
    CANCELLED,
    EXPIRED;

    public boolean isTerminal() {
        return this != PENDING_PEER_APPROVAL && this != PENDING_MANAGER_APPROVAL;
    }
}
