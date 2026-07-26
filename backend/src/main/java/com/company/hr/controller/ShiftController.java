package com.company.hr.controller;

import com.company.hr.model.dto.response.ShiftResponse;
import com.company.hr.model.entity.Employee;
import com.company.hr.security.CurrentUser;
import com.company.hr.service.ShiftService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** See SPEC.md "API Contract" > Employees & Shifts. */
@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @GetMapping("/me")
    public List<ShiftResponse> mine(
            @CurrentUser Employee employee,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return shiftService.listMine(employee, from, to);
    }

    @GetMapping("/{id}")
    public ShiftResponse getById(@CurrentUser Employee employee, @PathVariable UUID id) {
        return shiftService.getById(employee, id);
    }
}
