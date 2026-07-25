package com.company.hr.model.dto.response;

import com.company.hr.model.entity.EmployeeRole;

import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String employeeCode,
        String fullName,
        String email,
        EmployeeRole role,
        String departmentName,
        UUID managerId,
        boolean active) {
}
