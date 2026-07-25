package com.company.hr.service;

import com.company.hr.mapper.EmployeeMapper;
import com.company.hr.model.dto.response.EmployeeResponse;
import com.company.hr.model.entity.Employee;
import com.company.hr.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository, EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
    }

    @Override
    public EmployeeResponse getSelf(Employee employee) {
        return employeeMapper.toResponse(employee);
    }

    @Override
    public List<EmployeeResponse> getDirectReports(Employee manager) {
        return employeeRepository.findByManagerId(manager.getId()).stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Override
    public List<EmployeeResponse> getColleagues(Employee employee) {
        if (employee.getManager() == null) {
            return List.of();
        }
        return employeeRepository.findByManagerId(employee.getManager().getId()).stream()
                .filter(colleague -> !colleague.getId().equals(employee.getId()) && colleague.isActive())
                .map(employeeMapper::toResponse)
                .toList();
    }
}
