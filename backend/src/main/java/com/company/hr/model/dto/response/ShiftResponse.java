package com.company.hr.model.dto.response;

import com.company.hr.model.entity.ShiftStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ShiftResponse(
        UUID id,
        UUID employeeId,
        String employeeName,
        LocalDate shiftDate,
        LocalTime startTime,
        LocalTime endTime,
        ShiftStatus status) {
}
