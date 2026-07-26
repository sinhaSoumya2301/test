package com.company.hr.service;

import com.company.hr.exception.InvalidTransitionException;
import com.company.hr.exception.UnauthorizedActionException;
import com.company.hr.exception.UnprocessableEntityException;
import com.company.hr.mapper.ShiftSwapRequestMapper;
import com.company.hr.model.dto.request.CreateSwapRequestDto;
import com.company.hr.model.dto.request.DecisionDto;
import com.company.hr.model.dto.response.SwapRequestResponse;
import com.company.hr.model.entity.*;
import com.company.hr.repository.ApprovalRepository;
import com.company.hr.repository.EmployeeRepository;
import com.company.hr.repository.ShiftRepository;
import com.company.hr.repository.ShiftSwapRequestRepository;
import com.company.hr.util.DateUtil;
import com.company.hr.util.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the core swap-approval state machine (SPEC.md "Testing Strategy" > Backend unit).
 * Repositories/collaborators are mocked; DateUtil/ValidationUtil are used as real instances since
 * they're pure logic with no dependencies of their own.
 */
@ExtendWith(MockitoExtension.class)
class ShiftSwapServiceImplTest {

    @Mock private ShiftSwapRequestRepository shiftSwapRequestRepository;
    @Mock private ShiftRepository shiftRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ApprovalRepository approvalRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;
    @Mock private ShiftSwapRequestMapper mapper;

    private final DateUtil dateUtil = new DateUtil();
    private final ValidationUtil validationUtil = new ValidationUtil();

    private ShiftSwapServiceImpl service;

    private Employee manager;
    private Employee requester;
    private Employee target;

    @BeforeEach
    void setUp() {
        service = new ShiftSwapServiceImpl(
                shiftSwapRequestRepository, shiftRepository, employeeRepository, approvalRepository,
                notificationService, auditLogService, mapper, dateUtil, validationUtil);
        ReflectionTestUtils.setField(service, "expiryDays", 7L);

        manager = Employee.builder().id(UUID.randomUUID()).fullName("Mia Manager")
                .role(EmployeeRole.MANAGER).active(true).build();
        requester = Employee.builder().id(UUID.randomUUID()).fullName("Alice Requester")
                .role(EmployeeRole.EMPLOYEE).manager(manager).active(true).build();
        target = Employee.builder().id(UUID.randomUUID()).fullName("Bob Target")
                .role(EmployeeRole.EMPLOYEE).manager(manager).active(true).build();

        lenient().when(mapper.toResponse(any(), any())).thenAnswer(inv -> {
            ShiftSwapRequest r = inv.getArgument(0);
            return new SwapRequestResponse(r.getId(), r.getStatus(), requester.getId(), requester.getFullName(),
                    r.getRequesterShift() != null ? r.getRequesterShift().getId() : null,
                    target.getId(), target.getFullName(), null, r.getReason(), List.of(),
                    r.getCreatedAt(), r.getExpiresAt());
        });
    }

    private Shift futureShift(Employee owner) {
        return Shift.builder().id(UUID.randomUUID()).employee(owner)
                .shiftDate(LocalDate.now().plusDays(3)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(17, 0))
                .status(ShiftStatus.SCHEDULED).build();
    }

    @Test
    void create_rejectsSwapWithSelf() {
        Shift shift = futureShift(requester);
        when(shiftRepository.findById(shift.getId())).thenReturn(Optional.of(shift));

        CreateSwapRequestDto dto = new CreateSwapRequestDto(shift.getId(), requester.getId(), null, "reason");

        assertThatThrownBy(() -> service.create(requester, dto))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("yourself");
    }

    @Test
    void create_rejectsPastShift() {
        Shift pastShift = Shift.builder().id(UUID.randomUUID()).employee(requester)
                .shiftDate(LocalDate.now().minusDays(1)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(17, 0))
                .status(ShiftStatus.SCHEDULED).build();
        when(shiftRepository.findById(pastShift.getId())).thenReturn(Optional.of(pastShift));

        CreateSwapRequestDto dto = new CreateSwapRequestDto(pastShift.getId(), target.getId(), null, "reason");

        assertThatThrownBy(() -> service.create(requester, dto))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("already started");
    }

    @Test
    void create_rejectsWhenRequesterHasNoManager() {
        Employee noManagerRequester = Employee.builder().id(UUID.randomUUID()).fullName("Nomad")
                .role(EmployeeRole.EMPLOYEE).active(true).build();
        Shift shift = futureShift(noManagerRequester);
        when(shiftRepository.findById(shift.getId())).thenReturn(Optional.of(shift));
        when(employeeRepository.findById(target.getId())).thenReturn(Optional.of(target));

        CreateSwapRequestDto dto = new CreateSwapRequestDto(shift.getId(), target.getId(), null, "reason");

        assertThatThrownBy(() -> service.create(noManagerRequester, dto))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("no manager");
    }

    @Test
    void create_happyPath_setsPendingPeerApprovalAndNotifiesTarget() {
        Shift shift = futureShift(requester);
        when(shiftRepository.findById(shift.getId())).thenReturn(Optional.of(shift));
        when(employeeRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(shiftSwapRequestRepository.save(any(ShiftSwapRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(approvalRepository.findByShiftSwapRequestIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        CreateSwapRequestDto dto = new CreateSwapRequestDto(shift.getId(), target.getId(), null, "reason");
        SwapRequestResponse response = service.create(requester, dto);

        assertThat(response.status()).isEqualTo(SwapStatus.PENDING_PEER_APPROVAL);
        verify(notificationService).publish(eq(target.getId()), eq(NotificationType.SWAP_REQUESTED), any(), anyString());
        verify(auditLogService).record(eq("ShiftSwapRequest"), any(), eq("SWAP_CREATED"), eq(requester), isNull(), any());
    }

    @Test
    void peerDecision_onlyTargetMayDecide() {
        ShiftSwapRequest request = pendingPeerRequest();
        when(shiftSwapRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.peerDecision(requester, request.getId(), new DecisionDto(true, null)))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void peerDecision_acceptMovesToPendingManagerApprovalAndNotifiesBoth() {
        ShiftSwapRequest request = pendingPeerRequest();
        when(shiftSwapRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(shiftSwapRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(approvalRepository.findByShiftSwapRequestIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        SwapRequestResponse response = service.peerDecision(target, request.getId(), new DecisionDto(true, "sure"));

        assertThat(response.status()).isEqualTo(SwapStatus.PENDING_MANAGER_APPROVAL);
        verify(notificationService).publish(eq(requester.getId()), eq(NotificationType.SWAP_PEER_ACCEPTED), any(), anyString());
        verify(notificationService).publish(eq(manager.getId()), eq(NotificationType.SWAP_PEER_ACCEPTED), any(), anyString());
    }

    @Test
    void peerDecision_declineRejectsAndOnlyNotifiesRequester() {
        ShiftSwapRequest request = pendingPeerRequest();
        when(shiftSwapRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(shiftSwapRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(approvalRepository.findByShiftSwapRequestIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        SwapRequestResponse response = service.peerDecision(target, request.getId(), new DecisionDto(false, "can't"));

        assertThat(response.status()).isEqualTo(SwapStatus.REJECTED_BY_PEER);
        verify(notificationService).publish(eq(requester.getId()), eq(NotificationType.SWAP_PEER_DECLINED), any(), anyString());
        verify(notificationService, never()).publish(eq(target.getId()), any(), any(), anyString());
    }

    @Test
    void peerDecision_rejectsIfNotPendingPeerApproval() {
        ShiftSwapRequest request = pendingPeerRequest();
        request.setStatus(SwapStatus.CANCELLED);
        when(shiftSwapRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.peerDecision(target, request.getId(), new DecisionDto(true, null)))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void managerDecision_onlyRequestersActualManagerMayDecide() {
        ShiftSwapRequest request = pendingManagerRequest();
        when(shiftSwapRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        Employee impostor = Employee.builder().id(UUID.randomUUID()).fullName("Random Manager")
                .role(EmployeeRole.MANAGER).active(true).build();

        assertThatThrownBy(() -> service.managerDecision(impostor, request.getId(), new DecisionDto(true, null)))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void managerDecision_approveSwapsShiftOwnership() {
        ShiftSwapRequest request = pendingManagerRequest();
        when(shiftSwapRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(shiftSwapRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(shiftRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(approvalRepository.findByShiftSwapRequestIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        SwapRequestResponse response = service.managerDecision(manager, request.getId(), new DecisionDto(true, "ok"));

        assertThat(response.status()).isEqualTo(SwapStatus.APPROVED);
        assertThat(request.getRequesterShift().getEmployee()).isEqualTo(target);
        assertThat(request.getRequesterShift().getStatus()).isEqualTo(ShiftStatus.SWAPPED);
        verify(notificationService).publish(eq(requester.getId()), eq(NotificationType.SWAP_MANAGER_APPROVED), any(), anyString());
        verify(notificationService).publish(eq(target.getId()), eq(NotificationType.SWAP_MANAGER_APPROVED), any(), anyString());
    }

    @Test
    void managerDecision_rejectDoesNotTouchShiftsAndOnlyNotifiesRequester() {
        ShiftSwapRequest request = pendingManagerRequest();
        Employee originalOwner = request.getRequesterShift().getEmployee();
        when(shiftSwapRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(shiftSwapRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(approvalRepository.findByShiftSwapRequestIdOrderByCreatedAtAsc(any())).thenReturn(List.of());

        SwapRequestResponse response = service.managerDecision(manager, request.getId(), new DecisionDto(false, "no coverage"));

        assertThat(response.status()).isEqualTo(SwapStatus.REJECTED_BY_MANAGER);
        assertThat(request.getRequesterShift().getEmployee()).isEqualTo(originalOwner);
        verify(notificationService).publish(eq(requester.getId()), eq(NotificationType.SWAP_MANAGER_REJECTED), any(), anyString());
        verify(notificationService, never()).publish(eq(target.getId()), eq(NotificationType.SWAP_MANAGER_REJECTED), any(), anyString());
    }

    @Test
    void cancel_onlyRequesterMayCancel() {
        ShiftSwapRequest request = pendingPeerRequest();
        when(shiftSwapRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.cancel(target, request.getId()))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void cancel_rejectsTerminalState() {
        ShiftSwapRequest request = pendingPeerRequest();
        request.setStatus(SwapStatus.APPROVED);
        when(shiftSwapRequestRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.cancel(requester, request.getId()))
                .isInstanceOf(InvalidTransitionException.class);
    }

    private ShiftSwapRequest pendingPeerRequest() {
        Shift shift = futureShift(requester);
        return ShiftSwapRequest.builder()
                .id(UUID.randomUUID())
                .requester(requester)
                .requesterShift(shift)
                .targetEmployee(target)
                .status(SwapStatus.PENDING_PEER_APPROVAL)
                .reason("reason")
                .expiresAt(Instant.now().plusSeconds(60))
                .createdAt(Instant.now())
                .build();
    }

    private ShiftSwapRequest pendingManagerRequest() {
        ShiftSwapRequest request = pendingPeerRequest();
        request.setStatus(SwapStatus.PENDING_MANAGER_APPROVAL);
        return request;
    }
}
