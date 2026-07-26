package com.company.hr.controller;

import com.company.hr.model.dto.request.CreateSwapRequestDto;
import com.company.hr.model.dto.request.DecisionDto;
import com.company.hr.model.dto.response.PageResponse;
import com.company.hr.model.dto.response.SwapRequestResponse;
import com.company.hr.model.entity.Employee;
import com.company.hr.security.CurrentUser;
import com.company.hr.service.IdempotencyService;
import com.company.hr.service.ShiftSwapService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** See SPEC.md "API Contract" > Shift Swap Requests and docs/adr/0002. */
@RestController
@RequestMapping("/api/v1/shift-swaps")
public class ShiftSwapController {

    private static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    private final ShiftSwapService shiftSwapService;
    private final IdempotencyService idempotencyService;

    public ShiftSwapController(ShiftSwapService shiftSwapService, IdempotencyService idempotencyService) {
        this.shiftSwapService = shiftSwapService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public ResponseEntity<SwapRequestResponse> create(
            @CurrentUser Employee employee,
            @RequestHeader(value = HEADER_IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @Valid @RequestBody CreateSwapRequestDto dto) {
        SwapRequestResponse response = idempotencyService.execute(
                idempotencyKey, "POST /shift-swaps", employee, SwapRequestResponse.class,
                () -> shiftSwapService.create(employee, dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public PageResponse<SwapRequestResponse> mine(@CurrentUser Employee employee, Pageable pageable) {
        return shiftSwapService.listForEmployee(employee, pageable);
    }

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public PageResponse<SwapRequestResponse> team(@CurrentUser Employee manager, Pageable pageable) {
        return shiftSwapService.listForManager(manager, pageable);
    }

    @GetMapping("/{id}")
    public SwapRequestResponse getById(@CurrentUser Employee employee, @PathVariable UUID id) {
        return shiftSwapService.getById(employee, id);
    }

    @PostMapping("/{id}/peer-decision")
    public SwapRequestResponse peerDecision(
            @CurrentUser Employee employee,
            @PathVariable UUID id,
            @RequestHeader(value = HEADER_IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @Valid @RequestBody DecisionDto dto) {
        return idempotencyService.execute(
                idempotencyKey, "POST /shift-swaps/" + id + "/peer-decision", employee, SwapRequestResponse.class,
                () -> shiftSwapService.peerDecision(employee, id, dto));
    }

    @PostMapping("/{id}/manager-decision")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public SwapRequestResponse managerDecision(
            @CurrentUser Employee employee,
            @PathVariable UUID id,
            @RequestHeader(value = HEADER_IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @Valid @RequestBody DecisionDto dto) {
        return idempotencyService.execute(
                idempotencyKey, "POST /shift-swaps/" + id + "/manager-decision", employee, SwapRequestResponse.class,
                () -> shiftSwapService.managerDecision(employee, id, dto));
    }

    @PostMapping("/{id}/cancel")
    public SwapRequestResponse cancel(
            @CurrentUser Employee employee,
            @PathVariable UUID id,
            @RequestHeader(value = HEADER_IDEMPOTENCY_KEY, required = false) String idempotencyKey) {
        return idempotencyService.execute(
                idempotencyKey, "POST /shift-swaps/" + id + "/cancel", employee, SwapRequestResponse.class,
                () -> shiftSwapService.cancel(employee, id));
    }
}
