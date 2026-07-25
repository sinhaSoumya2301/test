package com.company.hr.security;

import com.company.hr.repository.EmployeeRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Username here is the employee's email (see login flow in service.AuthServiceImpl). Token
 * validation on subsequent requests loads by employee ID directly in JwtAuthenticationFilter —
 * this service is specifically for the initial login lookup.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public CustomUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return employeeRepository.findByEmailIgnoreCase(email)
                .map(EmployeePrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No employee with email " + email));
    }
}
