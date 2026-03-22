package com.ctse.registrationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RegistrationResponse DTO
 * =========================
 * Returned by:
 *   POST /registrations              → registration just created
 *   GET  /registrations/user/{userId} → list of user's registrations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationResponse {

    private UUID id;
    private String userId;
    private String eventId;
    private String status;
    private LocalDateTime createdAt;

    // Convenience message shown to the client after registering
    // e.g. "You have successfully registered for AI Workshop"
    private String message;
}
