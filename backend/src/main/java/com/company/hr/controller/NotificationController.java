package com.company.hr.controller;

import com.company.hr.model.dto.response.NotificationResponse;
import com.company.hr.model.dto.response.PageResponse;
import com.company.hr.model.entity.Employee;
import com.company.hr.security.CurrentUser;
import com.company.hr.service.NotificationService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** See SPEC.md "API Contract" > Notifications & Ops. */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @CurrentUser Employee employee,
            @RequestParam(defaultValue = "false") boolean unread,
            Pageable pageable) {
        return notificationService.list(employee, unread, pageable);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@CurrentUser Employee employee, @PathVariable UUID id) {
        notificationService.markRead(employee, id);
        return ResponseEntity.noContent().build();
    }
}
