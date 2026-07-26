package com.company.hr.service;

import com.company.hr.exception.ResourceNotFoundException;
import com.company.hr.mapper.ShiftMapper;
import com.company.hr.model.dto.response.ShiftResponse;
import com.company.hr.model.entity.Employee;
import com.company.hr.model.entity.Shift;
import com.company.hr.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;
    private final ShiftMapper shiftMapper;

    public ShiftServiceImpl(ShiftRepository shiftRepository, ShiftMapper shiftMapper) {
        this.shiftRepository = shiftRepository;
        this.shiftMapper = shiftMapper;
    }

    @Override
    public List<ShiftResponse> listMine(Employee employee, LocalDate from, LocalDate to) {
        return shiftRepository
                .findByEmployeeIdAndShiftDateBetweenOrderByShiftDateAscStartTimeAsc(employee.getId(), from, to)
                .stream()
                .map(shiftMapper::toResponse)
                .toList();
    }

    @Override
    public ShiftResponse getById(Employee actor, UUID shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("SHIFT_NOT_FOUND", "Shift not found."));

        boolean isOwner = shift.getEmployee().getId().equals(actor.getId());
        boolean isOwnersManager = shift.getEmployee().getManager() != null
                && shift.getEmployee().getManager().getId().equals(actor.getId());
        if (!isOwner && !isOwnersManager) {
            // Same "not found" used for both truly-missing and not-visible-to-caller (SPEC.md
            // API Contract conventions) — avoids confirming the shift exists to an unrelated caller.
            throw new ResourceNotFoundException("SHIFT_NOT_FOUND", "Shift not found.");
        }
        return shiftMapper.toResponse(shift);
    }
}
