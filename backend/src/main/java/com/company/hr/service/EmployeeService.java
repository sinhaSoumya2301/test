package com.company.hr.service;

import com.company.hr.model.dto.response.EmployeeResponse;
import com.company.hr.model.entity.Employee;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse getSelf(Employee employee);

    List<EmployeeResponse> getDirectReports(Employee manager);

    /** Other active employees sharing this employee's manager — who FR-2 lets them swap with. */
    List<EmployeeResponse> getColleagues(Employee employee);
}
