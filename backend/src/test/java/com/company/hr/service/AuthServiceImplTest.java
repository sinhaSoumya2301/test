package com.company.hr.service;

import com.company.hr.exception.InvalidCredentialsException;
import com.company.hr.model.dto.response.AuthTokenResponse;
import com.company.hr.model.entity.Employee;
import com.company.hr.model.entity.EmployeeRole;
import com.company.hr.model.entity.RefreshToken;
import com.company.hr.repository.EmployeeRepository;
import com.company.hr.repository.RefreshTokenRepository;
import com.company.hr.security.EmployeePrincipal;
import com.company.hr.security.JwtTokenProvider;
import com.company.hr.security.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests for login/refresh (SPEC.md "Testing Strategy" > auth: success/failure, refresh rotation/expiry). */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private TokenHasher tokenHasher;
    @Mock private AuditLogService auditLogService;
    @Mock private Authentication authentication;

    private AuthServiceImpl authService;
    private Employee employee;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                authenticationManager, employeeRepository, refreshTokenRepository,
                jwtTokenProvider, tokenHasher, auditLogService);
        employee = Employee.builder().id(UUID.randomUUID()).email("alice@example.com")
                .fullName("Alice").role(EmployeeRole.EMPLOYEE).active(true).build();
    }

    @Test
    void login_successIssuesTokensAndAudits() {
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new EmployeePrincipal(employee));
        when(jwtTokenProvider.generateAccessToken(employee)).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenTtlSeconds()).thenReturn(604800L);
        when(tokenHasher.generateOpaqueToken()).thenReturn("raw-refresh-token");
        when(tokenHasher.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");

        AuthTokenResponse response = authService.login("alice@example.com", "correct-password");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(response.expiresIn()).isEqualTo(900L);
        verify(refreshTokenRepository).save(argThat(rt -> rt.getTokenHash().equals("hashed-refresh-token")));
        verify(auditLogService).record(eq("Employee"), eq(employee.getId()), eq("LOGIN_SUCCESS"), eq(employee), isNull(), isNull());
    }

    @Test
    void login_badCredentialsAuditsFailureAndThrows() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad creds"));
        when(employeeRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> authService.login("alice@example.com", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(auditLogService).record(eq("Employee"), eq(employee.getId()), eq("LOGIN_FAILURE"), eq(employee), isNull(), isNull());
    }

    @Test
    void refresh_revokedTokenReuseRevokesWholeSessionAndThrows() {
        RefreshToken stored = RefreshToken.builder().id(UUID.randomUUID()).employee(employee)
                .tokenHash("hashed").expiresAt(Instant.now().plusSeconds(1000)).revoked(true).build();
        when(tokenHasher.hash("stolen-token")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.findByEmployeeIdAndRevokedFalse(employee.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> authService.refresh("stolen-token"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("revoked");

        verify(refreshTokenRepository).findByEmployeeIdAndRevokedFalse(employee.getId());
    }

    @Test
    void refresh_expiredTokenThrows() {
        RefreshToken stored = RefreshToken.builder().id(UUID.randomUUID()).employee(employee)
                .tokenHash("hashed").expiresAt(Instant.now().minusSeconds(10)).revoked(false).build();
        when(tokenHasher.hash("old-token")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("old-token"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void refresh_validTokenRotatesAndIssuesNewTokens() {
        RefreshToken stored = RefreshToken.builder().id(UUID.randomUUID()).employee(employee)
                .tokenHash("hashed-old").expiresAt(Instant.now().plusSeconds(1000)).revoked(false).build();
        when(tokenHasher.hash("valid-token")).thenReturn("hashed-old");
        when(refreshTokenRepository.findByTokenHash("hashed-old")).thenReturn(Optional.of(stored));
        when(jwtTokenProvider.generateAccessToken(employee)).thenReturn("new-access-token");
        when(jwtTokenProvider.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenTtlSeconds()).thenReturn(604800L);
        when(tokenHasher.generateOpaqueToken()).thenReturn("new-raw-refresh-token");
        when(tokenHasher.hash("new-raw-refresh-token")).thenReturn("hashed-new");

        AuthTokenResponse response = authService.refresh("valid-token");

        assertThat(stored.isRevoked()).isTrue();
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-raw-refresh-token");
    }
}
