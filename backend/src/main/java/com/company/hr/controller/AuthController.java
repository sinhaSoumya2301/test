package com.company.hr.controller;

import com.company.hr.mapper.EmployeeMapper;
import com.company.hr.model.dto.request.LoginRequest;
import com.company.hr.model.dto.request.RefreshRequest;
import com.company.hr.model.dto.response.AuthTokenResponse;
import com.company.hr.model.dto.response.EmployeeResponse;
import com.company.hr.model.entity.Employee;
import com.company.hr.security.CurrentUser;
import com.company.hr.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** See SPEC.md "API Contract" > Auth. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final EmployeeMapper employeeMapper;

    public AuthController(AuthService authService, EmployeeMapper employeeMapper) {
        this.authService = authService;
        this.employeeMapper = employeeMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthTokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> me(@CurrentUser Employee employee) {
        return ResponseEntity.status(HttpStatus.OK).body(employeeMapper.toResponse(employee));
    }
}
