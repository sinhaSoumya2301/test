package com.company.hr.controller;

import com.company.hr.model.dto.response.EmployeeResponse;
import com.company.hr.model.entity.Employee;
import com.company.hr.security.CurrentUser;
import com.company.hr.service.EmployeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** See SPEC.md "API Contract" > Employees & Shifts. */
@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/me")
    public EmployeeResponse me(@CurrentUser Employee employee) {
        return employeeService.getSelf(employee);
    }

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<EmployeeResponse> team(@CurrentUser Employee manager) {
        return employeeService.getDirectReports(manager);
    }

    // Not in the original SPEC.md endpoint table — added while building the swap-request UI
    // (task 7), since FR-2 requires picking a colleague to swap with and no endpoint existed
    // for that. See SPEC.md "API Contract" > Employees & Shifts, updated to match.
    @GetMapping("/colleagues")
    public List<EmployeeResponse> colleagues(@CurrentUser Employee employee) {
        return employeeService.getColleagues(employee);
    }
}
