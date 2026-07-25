package com.company.hr.mapper;

import com.company.hr.model.dto.response.EmployeeResponse;
import com.company.hr.model.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getRole(),
                employee.getDepartment() != null ? employee.getDepartment().getName() : null,
                employee.getManager() != null ? employee.getManager().getId() : null,
                employee.isActive());
    }
}
