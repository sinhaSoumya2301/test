package com.company.hr.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** See SPEC.md "API Contract" > Shift Swap Requests > POST /shift-swaps. */
public record CreateSwapRequestDto(
        @NotNull UUID requesterShiftId,
        @NotNull UUID targetEmployeeId,
        UUID targetShiftId,
        @NotBlank @Size(max = 1000) String reason) {
}
