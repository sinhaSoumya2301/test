package com.company.hr.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Unified shape for both the peer-decision and manager-decision endpoints — approve=true means
 * ACCEPT at the PEER stage and APPROVE at the MANAGER stage; approve=false means DECLINE /
 * REJECT respectively (both map to ApprovalDecision.REJECTED). See mapper usage in
 * ShiftSwapServiceImpl.
 */
public record DecisionDto(
        @NotNull Boolean approve,
        @Size(max = 1000) String comments) {
}
