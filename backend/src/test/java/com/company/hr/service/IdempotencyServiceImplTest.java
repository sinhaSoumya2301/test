package com.company.hr.service;

import com.company.hr.model.entity.Employee;
import com.company.hr.model.entity.EmployeeRole;
import com.company.hr.model.entity.IdempotencyRecord;
import com.company.hr.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Covers SPEC.md "Idempotency Testing": replay-on-retry and the concurrent-duplicate-insert race. */
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceImplTest {

    private record Dummy(String value) {
    }

    @Mock private IdempotencyRecordRepository repository;

    private IdempotencyServiceImpl service;
    private Employee employee;

    @BeforeEach
    void setUp() {
        service = new IdempotencyServiceImpl(repository, new ObjectMapper());
        ReflectionTestUtils.setField(service, "ttlHours", 24L);
        employee = Employee.builder().id(UUID.randomUUID()).fullName("Alice").role(EmployeeRole.EMPLOYEE).build();
    }

    @Test
    void nullKey_alwaysExecutesActionWithoutTouchingRepository() {
        AtomicInteger calls = new AtomicInteger();
        Dummy result = service.execute(null, "POST /x", employee, Dummy.class, () -> {
            calls.incrementAndGet();
            return new Dummy("fresh");
        });

        assertThat(result.value()).isEqualTo("fresh");
        assertThat(calls.get()).isEqualTo(1);
        verifyNoInteractions(repository);
    }

    @Test
    void freshCachedRecord_replaysWithoutReRunningAction() throws Exception {
        String cachedBody = new ObjectMapper().writeValueAsString(new Dummy("cached"));
        IdempotencyRecord record = IdempotencyRecord.builder()
                .id(UUID.randomUUID()).idempotencyKey("key-1").endpoint("POST /x").employee(employee)
                .responseStatus(200).responseBody(cachedBody).createdAt(Instant.now()).build();
        when(repository.findByIdempotencyKeyAndEndpointAndEmployeeId("key-1", "POST /x", employee.getId()))
                .thenReturn(Optional.of(record));

        AtomicInteger calls = new AtomicInteger();
        Dummy result = service.execute("key-1", "POST /x", employee, Dummy.class, () -> {
            calls.incrementAndGet();
            return new Dummy("should-not-run");
        });

        assertThat(result.value()).isEqualTo("cached");
        assertThat(calls.get()).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    void expiredRecord_deletesAndReExecutes() {
        IdempotencyRecord expired = IdempotencyRecord.builder()
                .id(UUID.randomUUID()).idempotencyKey("key-2").endpoint("POST /x").employee(employee)
                .responseStatus(200).responseBody("{}")
                .createdAt(Instant.now().minusSeconds(25 * 3600)).build();
        when(repository.findByIdempotencyKeyAndEndpointAndEmployeeId("key-2", "POST /x", employee.getId()))
                .thenReturn(Optional.of(expired));

        Dummy result = service.execute("key-2", "POST /x", employee, Dummy.class, () -> new Dummy("re-executed"));

        assertThat(result.value()).isEqualTo("re-executed");
        verify(repository).delete(expired);
        verify(repository).save(any());
    }

    @Test
    void concurrentInsertRace_replaysTheWinnerInsteadOfFailing() throws Exception {
        when(repository.findByIdempotencyKeyAndEndpointAndEmployeeId("key-3", "POST /x", employee.getId()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(IdempotencyRecord.builder()
                        .id(UUID.randomUUID()).idempotencyKey("key-3").endpoint("POST /x").employee(employee)
                        .responseStatus(200)
                        .responseBody(new ObjectMapper().writeValueAsString(new Dummy("winner")))
                        .createdAt(Instant.now()).build()));
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        Dummy result = service.execute("key-3", "POST /x", employee, Dummy.class, () -> new Dummy("mine"));

        assertThat(result.value()).isEqualTo("winner");
    }
}
