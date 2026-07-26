package com.company.hr.mapper;

import com.company.hr.model.dto.response.ApprovalResponse;
import com.company.hr.model.dto.response.SwapRequestResponse;
import com.company.hr.model.entity.Approval;
import com.company.hr.model.entity.ShiftSwapRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShiftSwapRequestMapper {

    public SwapRequestResponse toResponse(ShiftSwapRequest request, List<Approval> approvals) {
        return new SwapRequestResponse(
                request.getId(),
                request.getStatus(),
                request.getRequester().getId(),
                request.getRequester().getFullName(),
                request.getRequesterShift().getId(),
                request.getTargetEmployee().getId(),
                request.getTargetEmployee().getFullName(),
                request.getTargetShift() != null ? request.getTargetShift().getId() : null,
                request.getReason(),
                approvals.stream().map(this::toApprovalResponse).toList(),
                request.getCreatedAt(),
                request.getExpiresAt());
    }

    private ApprovalResponse toApprovalResponse(Approval approval) {
        return new ApprovalResponse(
                approval.getStage(),
                approval.getApprover().getId(),
                approval.getApprover().getFullName(),
                approval.getDecision(),
                approval.getNote(),
                approval.getDecidedAt());
    }
}
