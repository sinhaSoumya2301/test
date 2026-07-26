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
        return employeeMapper.toResponse(reattach(employee));
    }

    @Override
    public List<EmployeeResponse> getDirectReports(Employee manager) {
        return employeeRepository.findByManagerId(manager.getId()).stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Override
    public List<EmployeeResponse> getColleagues(Employee employee) {
        Employee fresh = reattach(employee);
        if (fresh.getManager() == null) {
            return List.of();
        }
        return employeeRepository.findByManagerId(fresh.getManager().getId()).stream()
                .filter(colleague -> !colleague.getId().equals(fresh.getId()) && colleague.isActive())
                .map(employeeMapper::toResponse)
                .toList();
    }

    /**
     * @CurrentUser injects the Employee loaded by JwtAuthenticationFilter, which runs its own
     * short-lived repository call with no Hibernate session held open into the controller/service
     * invocation — so that instance is detached and its lazy associations (department, manager)
     * cannot be initialized later. Re-fetching by ID here, inside this class's own
     * @Transactional method, gives back a session-bound instance whose lazy associations resolve
     * normally. Found via a real LazyInitializationException hitting GET /auth/me locally.
     */
    private Employee reattach(Employee employee) {
        return employeeRepository.findById(employee.getId()).orElse(employee);
    }
}
