package com.company.hr.service;

import com.company.hr.exception.InvalidCredentialsException;
import com.company.hr.model.dto.response.AuthTokenResponse;
import com.company.hr.model.entity.Employee;
import com.company.hr.model.entity.RefreshToken;
import com.company.hr.repository.EmployeeRepository;
import com.company.hr.repository.RefreshTokenRepository;
import com.company.hr.security.EmployeePrincipal;
import com.company.hr.security.JwtTokenProvider;
import com.company.hr.security.TokenHasher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final EmployeeRepository employeeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenHasher tokenHasher;
    private final AuditLogService auditLogService;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            EmployeeRepository employeeRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            TokenHasher tokenHasher,
            AuditLogService auditLogService) {
        this.authenticationManager = authenticationManager;
        this.employeeRepository = employeeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenHasher = tokenHasher;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public AuthTokenResponse login(String email, String rawPassword) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, rawPassword));
        } catch (BadCredentialsException | org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            Employee attempted = employeeRepository.findByEmailIgnoreCase(email).orElse(null);
            auditLogService.record("Employee", attempted != null ? attempted.getId() : null,
                    "LOGIN_FAILURE", attempted, null, null);
            throw new InvalidCredentialsException("Email or password is incorrect.");
        }

        Employee employee = ((EmployeePrincipal) authentication.getPrincipal()).getEmployee();
        AuthTokenResponse response = issueTokens(employee);
        auditLogService.record("Employee", employee.getId(), "LOGIN_SUCCESS", employee, null, null);
        return response;
    }

    @Override
    @Transactional
    public AuthTokenResponse refresh(String rawRefreshToken) {
        String hash = tokenHasher.hash(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token."));

        if (stored.isRevoked()) {
            // Reuse of an already-rotated-out token indicates possible theft — kill the whole
            // session rather than just this token (docs/adr/0003-jwt-access-refresh-auth.md).
            revokeAllForEmployee(stored.getEmployee());
            throw new InvalidCredentialsException("Refresh token has been revoked. Please log in again.");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException("Refresh token has expired. Please log in again.");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getEmployee());
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = tokenHasher.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private AuthTokenResponse issueTokens(Employee employee) {
        String accessToken = jwtTokenProvider.generateAccessToken(employee);
        String rawRefreshToken = tokenHasher.generateOpaqueToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .employee(employee)
                .tokenHash(tokenHasher.hash(rawRefreshToken))
                .expiresAt(Instant.now().plusSeconds(jwtTokenProvider.getRefreshTokenTtlSeconds()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return new AuthTokenResponse(accessToken, rawRefreshToken, jwtTokenProvider.getAccessTokenTtlSeconds());
    }

    private void revokeAllForEmployee(Employee employee) {
        List<RefreshToken> tokens = refreshTokenRepository.findByEmployeeIdAndRevokedFalse(employee.getId());
        tokens.forEach(t -> t.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);
    }
}
