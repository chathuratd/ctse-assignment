package com.ctse.registrationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * CreateRegistrationRequest DTO
 * ==============================
 * Request body for POST /registrations
 *
 * Spec:
 * {
 *   "userId": "user-123",
 *   "eventId": "event-456"
 * }
 *
 * The Registration Service will:
 *   1. Call User Service to validate userId exists
 *   2. Call Event Service to validate eventId exists
 *   3. Save the registration
 *   4. Call Notification Service to send confirmation
 */
@Data
public class CreateRegistrationRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "eventId is required")
    private String eventId;
}
