package com.ctse.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notification Entity
 * ===================
 * Represents a notification sent to a user.
 *
 * In this project, notifications are stored in the database and
 * retrieved via GET /notifications/user/{userId}.
 * (No email integration — stored notifications only, as per the spec.)
 *
 * Created by the Registration Service at step 4 of the registration flow:
 *   POST /notifications  { "userId": "...", "message": "You registered for AI Workshop" }
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    // userId is stored as a String (UUID value as text) — no FK constraint needed.
    // Microservices should not share DB schemas or have cross-service FK constraints.
    // This is the "shared nothing" principle: each service owns its data independently.
    @Column(nullable = false, length = 36)
    private String userId;

    // Notification type: WELCOME, NEW_EVENT, REGISTRATION_CONFIRMED
    // Used to build the message via template in NotificationService.
    @Column(nullable = false, length = 30)
    private String type;

    // eventId is only present for event-related notifications (NEW_EVENT, REGISTRATION_CONFIRMED).
    // Nullable — WELCOME notifications have no associated event.
    @Column(length = 36)
    private String eventId;

    // The final human-readable message, built from the template by NotificationService.
    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
