package com.ctse.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * NotificationResponse DTO
 * =========================
 * Returned by:
 *   POST /notifications              → notification just created
 *   GET  /notifications/user/{userId} → list of user's notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private String userId;
    private String type;
    private String message;
    private LocalDateTime createdAt;
}
