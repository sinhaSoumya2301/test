package com.company.hr.config;

import com.company.hr.security.EmployeePrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Enables Spring Data JPA auditing (@CreatedBy/@LastModifiedBy) for any entity that adopts it,
 * resolving the current actor from the JWT-authenticated SecurityContext. Distinct from the
 * business audit_log table (see model.entity.AuditLog / service.AuditLogService), which records
 * domain events (swap decisions, logins) rather than plain "who touched this row last".
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class DatabaseConfig {

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof EmployeePrincipal principal)) {
                return Optional.empty();
            }
            return Optional.of(principal.getEmployeeId());
        };
    }
}
