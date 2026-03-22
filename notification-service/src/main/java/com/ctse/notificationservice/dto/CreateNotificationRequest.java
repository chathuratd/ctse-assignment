package com.ctse.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * CreateNotificationRequest DTO
 * ==============================
 * Request body for POST /notifications
 *
 * Callers send a type and IDs — the Notification Service builds the message
 * by fetching details from User Service and Event Service.
 *
 * Examples:
 *   WELCOME:               { "type": "WELCOME",               "userId": "..." }
 *   NEW_EVENT:             { "type": "NEW_EVENT",             "userId": "...", "eventId": "..." }
 *   REGISTRATION_CONFIRMED:{ "type": "REGISTRATION_CONFIRMED","userId": "...", "eventId": "..." }
 */
@Data
public class CreateNotificationRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    // One of: WELCOME, NEW_EVENT, REGISTRATION_CONFIRMED
    @NotBlank(message = "type is required")
    private String type;

    // Required for NEW_EVENT and REGISTRATION_CONFIRMED; omit for WELCOME
    private String eventId;
}
