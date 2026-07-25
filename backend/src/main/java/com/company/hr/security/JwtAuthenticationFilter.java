package com.company.hr.security;

import com.company.hr.model.entity.Employee;
import com.company.hr.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates the Bearer access token on every request and, if valid, populates the
 * SecurityContext with an EmployeePrincipal so downstream @PreAuthorize checks and controllers
 * can rely on Spring Security's normal authentication/authorization machinery.
 *
 * Deliberately does NOT reject invalid/missing tokens itself — it just leaves the
 * SecurityContext empty, so the request continues either to a permitAll endpoint or gets
 * rejected downstream by RestAuthenticationEntryPoint when authorization is actually required.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final EmployeeRepository employeeRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, EmployeeRepository employeeRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtTokenProvider.parseAndValidate(token);
                UUID employeeId = jwtTokenProvider.extractEmployeeId(claims);
                Optional<Employee> employee = employeeRepository.findById(employeeId);
                if (employee.isPresent() && employee.get().isActive()) {
                    EmployeePrincipal principal = new EmployeePrincipal(employee.get());
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("Rejected invalid access token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
