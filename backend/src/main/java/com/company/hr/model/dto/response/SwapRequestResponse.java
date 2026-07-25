package com.company.hr.model.dto.response;

import com.company.hr.model.entity.SwapStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SwapRequestResponse(
        UUID id,
        SwapStatus status,
        UUID requesterId,
        String requesterName,
        UUID requesterShiftId,
        UUID targetEmployeeId,
        String targetEmployeeName,
        UUID targetShiftId,
        String reason,
        List<ApprovalResponse> approvals,
        Instant createdAt,
        Instant expiresAt) {
}
