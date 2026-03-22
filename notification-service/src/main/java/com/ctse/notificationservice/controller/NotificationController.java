package com.ctse.notificationservice.controller;

import com.ctse.notificationservice.dto.CreateNotificationRequest;
import com.ctse.notificationservice.dto.NotificationResponse;
import com.ctse.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NotificationController — REST Endpoints for Notifications
 * ===========================================================
 *
 * Endpoints (matching the system API spec):
 *
 *   POST /notifications              Create a notification (called by Registration Service)
 *   GET  /notifications/user/{userId} Get all notifications for a user (called by client)
 *
 * INTER-SERVICE COMMUNICATION:
 * ─────────────────────────────
 * This controller IS the target of an inter-service call.
 * Registration Service sends POST /notifications after saving a registration.
 *
 * Communication flow:
 *   Client → POST /registrations → Registration Service
 *                                       ↓
 *                               POST /notifications → Notification Service (this)
 *                                       ↓
 *                               Stores: "You registered for AI Workshop"
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * POST /notifications
     * Called by Registration Service with:
     * { "userId": "uuid", "message": "You have successfully registered for AI Workshop" }
     */
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService.createNotification(request));
    }

    /**
     * GET /notifications/user/{userId}
     * Returns all notifications for a given user, newest first.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(
            @PathVariable String userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }
}
