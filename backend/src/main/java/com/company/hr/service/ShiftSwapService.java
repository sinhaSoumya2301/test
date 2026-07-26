package com.company.hr.service;

import com.company.hr.model.dto.request.CreateSwapRequestDto;
import com.company.hr.model.dto.request.DecisionDto;
import com.company.hr.model.dto.response.PageResponse;
import com.company.hr.model.dto.response.SwapRequestResponse;
import com.company.hr.model.entity.Employee;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/** See SPEC.md "API Contract" > Shift Swap Requests and docs/adr/0002. */
public interface ShiftSwapService {

    SwapRequestResponse create(Employee requester, CreateSwapRequestDto dto);

    SwapRequestResponse peerDecision(Employee actor, UUID requestId, DecisionDto dto);

    SwapRequestResponse managerDecision(Employee actor, UUID requestId, DecisionDto dto);

    SwapRequestResponse cancel(Employee actor, UUID requestId);

    SwapRequestResponse getById(Employee actor, UUID requestId);

    PageResponse<SwapRequestResponse> listForEmployee(Employee actor, Pageable pageable);

    PageResponse<SwapRequestResponse> listForManager(Employee actor, Pageable pageable);

    /** Invoked by service.ExpiryScheduler — marks stale PENDING_* requests EXPIRED. */
    void expireStale();
}
