package com.company.hr.model.event;

import com.company.hr.model.entity.NotificationType;

import java.util.UUID;

/**
 * Published (not directly acted on) inside a business transaction — see
 * service.NotificationServiceImpl#onNotificationEvent, which only runs after that transaction
 * commits. This is deliberate: dispatching a notification for a just-created
 * ShiftSwapRequest before the outer transaction commits would try to persist a Notification
 * referencing a row the async listener's own (separate) transaction can't see yet.
 */
public record NotificationEvent(
        UUID recipientId,
        NotificationType type,
        UUID relatedSwapRequestId,
        String message) {
}
