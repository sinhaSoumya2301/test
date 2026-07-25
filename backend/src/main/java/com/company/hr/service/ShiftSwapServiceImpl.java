package com.company.hr.service;

import com.company.hr.exception.InvalidTransitionException;
import com.company.hr.exception.ResourceNotFoundException;
import com.company.hr.exception.UnauthorizedActionException;
import com.company.hr.exception.UnprocessableEntityException;
import com.company.hr.mapper.ShiftSwapRequestMapper;
import com.company.hr.model.dto.request.CreateSwapRequestDto;
import com.company.hr.model.dto.request.DecisionDto;
import com.company.hr.model.dto.response.PageResponse;
import com.company.hr.model.dto.response.SwapRequestResponse;
import com.company.hr.model.entity.*;
import com.company.hr.repository.ApprovalRepository;
import com.company.hr.repository.EmployeeRepository;
import com.company.hr.repository.ShiftRepository;
import com.company.hr.repository.ShiftSwapRequestRepository;
import com.company.hr.util.DateUtil;
import com.company.hr.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ShiftSwapServiceImpl implements ShiftSwapService {

    private final ShiftSwapRequestRepository shiftSwapRequestRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final ApprovalRepository approvalRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ShiftSwapRequestMapper mapper;
    private final DateUtil dateUtil;
    private final ValidationUtil validationUtil;

    @Value("${app.swap.request-expiry-days}")
    private long expiryDays;

    public ShiftSwapServiceImpl(
            ShiftSwapRequestRepository shiftSwapRequestRepository,
            ShiftRepository shiftRepository,
            EmployeeRepository employeeRepository,
            ApprovalRepository approvalRepository,
            NotificationService notificationService,
            AuditLogService auditLogService,
            ShiftSwapRequestMapper mapper,
            DateUtil dateUtil,
            ValidationUtil validationUtil) {
        this.shiftSwapRequestRepository = shiftSwapRequestRepository;
        this.shiftRepository = shiftRepository;
        this.employeeRepository = employeeRepository;
        this.approvalRepository = approvalRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.mapper = mapper;
        this.dateUtil = dateUtil;
        this.validationUtil = validationUtil;
    }

    @Override
    @Transactional
    public SwapRequestResponse create(Employee requester, CreateSwapRequestDto dto) {
        Shift requesterShift = shiftRepository.findById(dto.requesterShiftId())
                .filter(s -> s.getEmployee().getId().equals(requester.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("SHIFT_NOT_FOUND", "Shift not found."));
        validationUtil.requireTrue(requesterShift.getStatus() == ShiftStatus.SCHEDULED,
                "SHIFT_NOT_SCHEDULED", "This shift is not currently scheduled.");
        validationUtil.requireTrue(dateUtil.isFuture(requesterShift.getShiftDate(), requesterShift.getStartTime()),
                "SHIFT_NOT_FUTURE", "Cannot swap a shift that has already started.");

        validationUtil.requireDifferent(requester.getId(), dto.targetEmployeeId(),
                "CANNOT_SWAP_WITH_SELF", "You cannot swap a shift with yourself.");

        Employee targetEmployee = employeeRepository.findById(dto.targetEmployeeId())
                .filter(Employee::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("TARGET_EMPLOYEE_NOT_FOUND", "Target employee not found."));

        Shift targetShift = null;
        if (dto.targetShiftId() != null) {
            targetShift = shiftRepository.findById(dto.targetShiftId())
                    .filter(s -> s.getEmployee().getId().equals(targetEmployee.getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("SHIFT_NOT_FOUND", "Target shift not found."));
            validationUtil.requireTrue(targetShift.getStatus() == ShiftStatus.SCHEDULED,
                    "SHIFT_NOT_SCHEDULED", "The target shift is not currently scheduled.");
            validationUtil.requireTrue(dateUtil.isFuture(targetShift.getShiftDate(), targetShift.getStartTime()),
                    "SHIFT_NOT_FUTURE", "Cannot swap for a shift that has already started.");
        }

        if (requester.getManager() == null) {
            throw new UnprocessableEntityException("NO_MANAGER_ASSIGNED",
                    "You have no manager assigned, so this request cannot be routed for approval.");
        }

        ShiftSwapRequest request = ShiftSwapRequest.builder()
                .requester(requester)
                .requesterShift(requesterShift)
                .targetEmployee(targetEmployee)
                .targetShift(targetShift)
                .status(SwapStatus.PENDING_PEER_APPROVAL)
                .reason(dto.reason())
                .expiresAt(Instant.now().plus(expiryDays, ChronoUnit.DAYS))
                .build();
        request = shiftSwapRequestRepository.save(request);

        auditLogService.record("ShiftSwapRequest", request.getId(), "SWAP_CREATED", requester,
                null, Map.of("status", request.getStatus().name()));
        notificationService.publish(targetEmployee.getId(), NotificationType.SWAP_REQUESTED, request.getId(),
                requester.getFullName() + " requested to swap a shift with you.");

        return toResponse(request);
    }

    @Override
    @Transactional
    public SwapRequestResponse peerDecision(Employee actor, UUID requestId, DecisionDto dto) {
        ShiftSwapRequest request = loadOrThrow(requestId);

        if (!request.getTargetEmployee().getId().equals(actor.getId())) {
            throw new UnauthorizedActionException("NOT_SWAP_TARGET",
                    "Only the target employee can accept or decline this request.");
        }
        if (request.getStatus() != SwapStatus.PENDING_PEER_APPROVAL) {
            throw new InvalidTransitionException("NOT_PENDING_PEER_APPROVAL",
                    "This request is no longer pending peer approval.");
        }

        ApprovalDecision decision = dto.approve() ? ApprovalDecision.APPROVED : ApprovalDecision.REJECTED;
        Map<String, Object> oldValue = Map.of("status", request.getStatus().name());

        try {
            saveApproval(request, ApprovalStage.PEER, actor, decision, dto.comments());

            if (decision == ApprovalDecision.APPROVED) {
                Employee manager = request.getRequester().getManager();
                if (manager == null) {
                    throw new UnprocessableEntityException("NO_MANAGER_ASSIGNED",
                            "The requester no longer has a manager assigned; this request cannot proceed.");
                }
                request.setStatus(SwapStatus.PENDING_MANAGER_APPROVAL);
                shiftSwapRequestRepository.save(request);

                auditLogService.record("ShiftSwapRequest", request.getId(), "SWAP_PEER_ACCEPTED", actor,
                        oldValue, Map.of("status", request.getStatus().name()));
                notificationService.publish(request.getRequester().getId(), NotificationType.SWAP_PEER_ACCEPTED,
                        request.getId(), actor.getFullName() + " accepted your swap request. Awaiting manager approval.");
                notificationService.publish(manager.getId(), NotificationType.SWAP_PEER_ACCEPTED, request.getId(),
                        "A shift swap request from " + request.getRequester().getFullName() + " needs your approval.");
            } else {
                request.setStatus(SwapStatus.REJECTED_BY_PEER);
                shiftSwapRequestRepository.save(request);

                auditLogService.record("ShiftSwapRequest", request.getId(), "SWAP_PEER_DECLINED", actor,
                        oldValue, Map.of("status", request.getStatus().name()));
                notificationService.publish(request.getRequester().getId(), NotificationType.SWAP_PEER_DECLINED,
                        request.getId(), actor.getFullName() + " declined your swap request.");
            }
        } catch (OptimisticLockingFailureException | DataIntegrityViolationException e) {
            // Lost a race against a concurrent decision on the same request (SPEC.md
            // "Idempotency Testing" — the state-machine guard as defense in depth).
            throw new InvalidTransitionException("ALREADY_DECIDED",
                    "This request was already decided concurrently.");
        }

        return toResponse(request);
    }

    @Override
    @Transactional
    public SwapRequestResponse managerDecision(Employee actor, UUID requestId, DecisionDto dto) {
        ShiftSwapRequest request = loadOrThrow(requestId);

        Employee actualManager = request.getRequester().getManager();
        if (actualManager == null || !actualManager.getId().equals(actor.getId())) {
            throw new UnauthorizedActionException("NOT_REQUESTERS_MANAGER",
                    "You are not authorized to decide this request.");
        }
        if (request.getStatus() != SwapStatus.PENDING_MANAGER_APPROVAL) {
            throw new InvalidTransitionException("ALREADY_DECIDED",
                    "This request is no longer pending manager approval.");
        }

        ApprovalDecision decision = dto.approve() ? ApprovalDecision.APPROVED : ApprovalDecision.REJECTED;
        Map<String, Object> oldValue = Map.of("status", request.getStatus().name());

        try {
            saveApproval(request, ApprovalStage.MANAGER, actor, decision, dto.comments());

            if (decision == ApprovalDecision.APPROVED) {
                applyShiftSwap(request);
                request.setStatus(SwapStatus.APPROVED);
                shiftSwapRequestRepository.save(request);

                auditLogService.record("ShiftSwapRequest", request.getId(), "SWAP_MANAGER_APPROVED", actor,
                        oldValue, Map.of("status", request.getStatus().name()));
                notificationService.publish(request.getRequester().getId(), NotificationType.SWAP_MANAGER_APPROVED,
                        request.getId(), "Your shift swap request was approved.");
                notificationService.publish(request.getTargetEmployee().getId(), NotificationType.SWAP_MANAGER_APPROVED,
                        request.getId(), "A shift swap involving you was approved.");
            } else {
                request.setStatus(SwapStatus.REJECTED_BY_MANAGER);
                shiftSwapRequestRepository.save(request);

                auditLogService.record("ShiftSwapRequest", request.getId(), "SWAP_MANAGER_REJECTED", actor,
                        oldValue, Map.of("status", request.getStatus().name()));
                notificationService.publish(request.getRequester().getId(), NotificationType.SWAP_MANAGER_REJECTED,
                        request.getId(), "Your shift swap request was rejected by your manager.");
            }
        } catch (OptimisticLockingFailureException | DataIntegrityViolationException e) {
            throw new InvalidTransitionException("ALREADY_DECIDED",
                    "This request was already decided concurrently.");
        }

        return toResponse(request);
    }

    // Atomic by virtue of running inside this @Transactional method (SPEC.md NFR "Data integrity").
    private void applyShiftSwap(ShiftSwapRequest request) {
        Shift requesterShift = request.getRequesterShift();
        Shift targetShift = request.getTargetShift();
        Employee requester = requesterShift.getEmployee();
        Employee target = request.getTargetEmployee();

        requesterShift.setEmployee(target);
        requesterShift.setStatus(ShiftStatus.SWAPPED);
        shiftRepository.save(requesterShift);

        if (targetShift != null) {
            targetShift.setEmployee(requester);
            targetShift.setStatus(ShiftStatus.SWAPPED);
            shiftRepository.save(targetShift);
        }
    }

    @Override
    @Transactional
    public SwapRequestResponse cancel(Employee actor, UUID requestId) {
        ShiftSwapRequest request = loadOrThrow(requestId);
        if (!request.getRequester().getId().equals(actor.getId())) {
            throw new UnauthorizedActionException("NOT_REQUESTER", "Only the requester can cancel this request.");
        }
        if (request.getStatus().isTerminal()) {
            throw new InvalidTransitionException("ALREADY_TERMINAL",
                    "This request has already reached a final state and cannot be cancelled.");
        }

        Map<String, Object> oldValue = Map.of("status", request.getStatus().name());
        request.setStatus(SwapStatus.CANCELLED);
        shiftSwapRequestRepository.save(request);

        auditLogService.record("ShiftSwapRequest", request.getId(), "SWAP_CANCELLED", actor,
                oldValue, Map.of("status", request.getStatus().name()));
        notificationService.publish(request.getTargetEmployee().getId(), NotificationType.SWAP_CANCELLED,
                request.getId(), request.getRequester().getFullName() + " cancelled their shift swap request.");

        return toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public SwapRequestResponse getById(Employee actor, UUID requestId) {
        ShiftSwapRequest request = loadOrThrow(requestId);
        if (!isVisibleTo(request, actor)) {
            throw new ResourceNotFoundException("SWAP_REQUEST_NOT_FOUND", "Shift swap request not found.");
        }
        return toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SwapRequestResponse> listForEmployee(Employee actor, Pageable pageable) {
        Page<ShiftSwapRequest> page = shiftSwapRequestRepository.findAllForEmployee(actor.getId(), pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SwapRequestResponse> listForManager(Employee actor, Pageable pageable) {
        Page<ShiftSwapRequest> page = shiftSwapRequestRepository.findAllForManager(actor.getId(), pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Override
    @Transactional
    public void expireStale() {
        List<ShiftSwapRequest> stale = shiftSwapRequestRepository.findByStatusInAndExpiresAtBefore(
                List.of(SwapStatus.PENDING_PEER_APPROVAL, SwapStatus.PENDING_MANAGER_APPROVAL), Instant.now());
        for (ShiftSwapRequest request : stale) {
            Map<String, Object> oldValue = Map.of("status", request.getStatus().name());
            request.setStatus(SwapStatus.EXPIRED);
            shiftSwapRequestRepository.save(request);

            auditLogService.record("ShiftSwapRequest", request.getId(), "SWAP_EXPIRED", null,
                    oldValue, Map.of("status", "EXPIRED"));
            notificationService.publish(request.getRequester().getId(), NotificationType.SWAP_EXPIRED,
                    request.getId(), "Your shift swap request expired without a decision.");
        }
    }

    private void saveApproval(ShiftSwapRequest request, ApprovalStage stage, Employee approver,
                               ApprovalDecision decision, String note) {
        Approval approval = Approval.builder()
                .shiftSwapRequest(request)
                .stage(stage)
                .approver(approver)
                .decision(decision)
                .note(note)
                .decidedAt(Instant.now())
                .build();
        approvalRepository.save(approval);
    }

    private boolean isVisibleTo(ShiftSwapRequest request, Employee actor) {
        return request.getRequester().getId().equals(actor.getId())
                || request.getTargetEmployee().getId().equals(actor.getId())
                || (request.getRequester().getManager() != null
                        && request.getRequester().getManager().getId().equals(actor.getId()));
    }

    private ShiftSwapRequest loadOrThrow(UUID requestId) {
        return shiftSwapRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("SWAP_REQUEST_NOT_FOUND", "Shift swap request not found."));
    }

    private SwapRequestResponse toResponse(ShiftSwapRequest request) {
        List<Approval> approvals = approvalRepository.findByShiftSwapRequestIdOrderByCreatedAtAsc(request.getId());
        return mapper.toResponse(request, approvals);
    }
}
