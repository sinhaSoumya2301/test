package com.company.hr.mapper;

import com.company.hr.model.dto.response.NotificationResponse;
import com.company.hr.model.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getRelatedSwapRequest() != null ? notification.getRelatedSwapRequest().getId() : null,
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
