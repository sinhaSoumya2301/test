package com.company.hr.service;

import com.company.hr.model.dto.response.ShiftResponse;
import com.company.hr.model.entity.Employee;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ShiftService {

    List<ShiftResponse> listMine(Employee employee, LocalDate from, LocalDate to);

    ShiftResponse getById(Employee actor, UUID shiftId);
}
