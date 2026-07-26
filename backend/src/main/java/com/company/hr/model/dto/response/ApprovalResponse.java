package com.company.hr.model.dto.response;

import com.company.hr.model.entity.ApprovalDecision;
import com.company.hr.model.entity.ApprovalStage;

import java.time.Instant;
import java.util.UUID;

public record ApprovalResponse(
        ApprovalStage stage,
        UUID approverId,
        String approverName,
        ApprovalDecision decision,
        String note,
        Instant decidedAt) {
}
