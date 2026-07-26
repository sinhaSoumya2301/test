package com.company.hr.service;

import com.company.hr.exception.ResourceNotFoundException;
import com.company.hr.mapper.NotificationMapper;
import com.company.hr.model.dto.response.NotificationResponse;
import com.company.hr.model.dto.response.PageResponse;
import com.company.hr.model.entity.Employee;
import com.company.hr.model.entity.Notification;
import com.company.hr.model.entity.NotificationType;
import com.company.hr.model.entity.ShiftSwapRequest;
import com.company.hr.model.event.NotificationEvent;
import com.company.hr.repository.EmployeeRepository;
import com.company.hr.repository.NotificationRepository;
import com.company.hr.repository.ShiftSwapRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final ApplicationEventPublisher eventPublisher;
    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    private final ShiftSwapRequestRepository shiftSwapRequestRepository;
    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(
            ApplicationEventPublisher eventPublisher,
            NotificationRepository notificationRepository,
            EmployeeRepository employeeRepository,
            ShiftSwapRequestRepository shiftSwapRequestRepository,
            NotificationMapper notificationMapper) {
        this.eventPublisher = eventPublisher;
        this.notificationRepository = notificationRepository;
        this.employeeRepository = employeeRepository;
        this.shiftSwapRequestRepository = shiftSwapRequestRepository;
        this.notificationMapper = notificationMapper;
    }

    @Override
    public void publish(UUID recipientId, NotificationType type, UUID relatedSwapRequestId, String message) {
        eventPublisher.publishEvent(new NotificationEvent(recipientId, type, relatedSwapRequestId, message));
    }

    // fallbackExecution=true so a publish() call outside an active transaction (shouldn't
    // happen in practice, but this is a defensive default) still dispatches rather than
    // silently vanishing.
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNotificationEvent(NotificationEvent event) {
        Employee recipient = employeeRepository.findById(event.recipientId()).orElse(null);
        if (recipient == null) {
            log.warn("Notification recipient {} no longer exists, dropping notification", event.recipientId());
            return;
        }
        ShiftSwapRequest related = event.relatedSwapRequestId() != null
                ? shiftSwapRequestRepository.findById(event.relatedSwapRequestId()).orElse(null)
                : null;

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(event.type())
                .relatedSwapRequest(related)
                .message(event.message())
                .read(false)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    public PageResponse<NotificationResponse> list(Employee recipient, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipient.getId(), pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipient.getId(), pageable);
        return PageResponse.of(page, notificationMapper::toResponse);
    }

    @Override
    public void markRead(Employee recipient, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getRecipient().getId().equals(recipient.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("NOTIFICATION_NOT_FOUND", "Notification not found."));
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}
