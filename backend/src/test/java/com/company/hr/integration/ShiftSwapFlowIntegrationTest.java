package com.company.hr.integration;

import com.company.hr.model.dto.request.CreateSwapRequestDto;
import com.company.hr.model.entity.Employee;
import com.company.hr.model.entity.EmployeeRole;
import com.company.hr.model.entity.Shift;
import com.company.hr.model.entity.ShiftStatus;
import com.company.hr.repository.EmployeeRepository;
import com.company.hr.repository.ShiftRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full HTTP happy path (SPEC.md "Testing Strategy" > Backend integration): create → peer-accept
 * → manager-approve, plus a 403 edge case. Requires Docker (Testcontainers Postgres) — runs in
 * CI; skipped locally if no Docker daemon is available.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Testcontainers
class ShiftSwapFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private Employee manager;
    private Employee requester;
    private Employee target;
    private Shift requesterShift;

    private static final String PASSWORD = "password123";

    @BeforeEach
    void seed() {
        manager = employeeRepository.save(Employee.builder()
                .employeeCode("MGR-" + UUID.randomUUID())
                .fullName("Mia Manager")
                .email("mia.manager." + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(EmployeeRole.MANAGER).active(true).build());

        requester = employeeRepository.save(Employee.builder()
                .employeeCode("EMP-" + UUID.randomUUID())
                .fullName("Alice Requester")
                .email("alice." + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(EmployeeRole.EMPLOYEE).manager(manager).active(true).build());

        target = employeeRepository.save(Employee.builder()
                .employeeCode("EMP-" + UUID.randomUUID())
                .fullName("Bob Target")
                .email("bob." + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .role(EmployeeRole.EMPLOYEE).manager(manager).active(true).build());

        requesterShift = shiftRepository.save(Shift.builder()
                .employee(requester)
                .shiftDate(LocalDate.now().plusDays(5))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(17, 0))
                .status(ShiftStatus.SCHEDULED).build());
    }

    private record LoginPayload(String email, String password) {
    }

    private String loginAndGetToken(String email) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginPayload(email, PASSWORD));
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void fullApprovalFlow_createPeerAcceptManagerApprove() throws Exception {
        String requesterToken = loginAndGetToken(requester.getEmail());
        String targetToken = loginAndGetToken(target.getEmail());
        String managerToken = loginAndGetToken(manager.getEmail());

        String createBody = objectMapper.writeValueAsString(
                new CreateSwapRequestDto(requesterShift.getId(), target.getId(), null, "Family event"));

        String createResponse = mockMvc.perform(post("/api/v1/shift-swaps")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType("application/json").content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", equalTo("PENDING_PEER_APPROVAL")))
                .andReturn().getResponse().getContentAsString();
        String requestId = objectMapper.readTree(createResponse).get("id").asText();

        // The requester is not the target — may not decide their own request (403).
        mockMvc.perform(post("/api/v1/shift-swaps/" + requestId + "/peer-decision")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType("application/json").content("{\"approve\": true}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/shift-swaps/" + requestId + "/peer-decision")
                        .header("Authorization", "Bearer " + targetToken)
                        .contentType("application/json").content("{\"approve\": true, \"comments\": \"sure\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("PENDING_MANAGER_APPROVAL")));

        mockMvc.perform(post("/api/v1/shift-swaps/" + requestId + "/manager-decision")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType("application/json").content("{\"approve\": true, \"comments\": \"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("APPROVED")));
    }
}
