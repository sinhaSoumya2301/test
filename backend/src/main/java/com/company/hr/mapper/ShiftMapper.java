package com.company.hr.mapper;

import com.company.hr.model.dto.response.ShiftResponse;
import com.company.hr.model.entity.Shift;
import org.springframework.stereotype.Component;

@Component
public class ShiftMapper {

    public ShiftResponse toResponse(Shift shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getEmployee().getId(),
                shift.getEmployee().getFullName(),
                shift.getShiftDate(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getStatus());
    }
}
