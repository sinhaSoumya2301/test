package com.company.hr.service;

import com.company.hr.model.dto.response.NotificationResponse;
import com.company.hr.model.dto.response.PageResponse;
import com.company.hr.model.entity.Employee;
import com.company.hr.model.entity.NotificationType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    /**
     * Publishes a NotificationEvent for async, after-commit dispatch (see AsyncConfig and
     * model.event.NotificationEvent) — never persists synchronously within the caller's
     * transaction.
     */
    void publish(UUID recipientId, NotificationType type, UUID relatedSwapRequestId, String message);

    PageResponse<NotificationResponse> list(Employee recipient, boolean unreadOnly, Pageable pageable);

    void markRead(Employee recipient, UUID notificationId);
}
