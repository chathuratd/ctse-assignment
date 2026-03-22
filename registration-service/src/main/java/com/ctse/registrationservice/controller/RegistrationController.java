package com.ctse.registrationservice.controller;

import com.ctse.registrationservice.dto.CreateRegistrationRequest;
import com.ctse.registrationservice.dto.RegistrationResponse;
import com.ctse.registrationservice.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * RegistrationController — REST Endpoints for Event Registrations
 * ================================================================
 *
 * Endpoints (matching the system API spec):
 *
 *   POST   /registrations               Register user for event
 *   GET    /registrations/user/{userId} Get all registrations for a user
 *   DELETE /registrations/{id}          Cancel a registration
 *
 * This controller is the ENTRY POINT for the most important workflow:
 *
 *   Client → POST /registrations → [this controller]
 *                                      ↓
 *                              RegistrationService orchestrates:
 *                                1. GET user-service/users/{userId}
 *                                2. GET event-service/events/{eventId}
 *                                3. Save to registrations table
 *                                4. POST notification-service/notifications
 *
 * This single POST call to ONE endpoint triggers calls to THREE other services.
 * This is what "microservice orchestration" looks like in practice.
 */
@RestController
@RequestMapping("/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * POST /registrations
     *
     * The core business operation. Validates user + event, creates registration,
     * triggers notification — all in one request.
     *
     * Request:  { "userId": "uuid", "eventId": "uuid" }
     * Response: { "id": "uuid", "status": "CONFIRMED", "message": "Registration successful for AI Workshop" }
     */
    @PostMapping
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody CreateRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.register(request));
    }

    /**
     * GET /registrations/user/{userId}
     *
     * Returns all registrations for a given user.
     * Shows which events the user has signed up for.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RegistrationResponse>> getUserRegistrations(
            @PathVariable String userId) {
        return ResponseEntity.ok(registrationService.getUserRegistrations(userId));
    }

    /**
     * DELETE /registrations/{id}
     *
     * Cancels an existing registration (soft delete — sets status to CANCELLED).
     * Returns the updated registration with status: "CANCELLED".
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<RegistrationResponse> cancelRegistration(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.cancelRegistration(id));
    }
}
