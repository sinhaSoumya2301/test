package com.company.hr.model.dto.response;

import com.company.hr.model.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        UUID relatedSwapRequestId,
        String message,
        boolean read,
        Instant createdAt) {
}
