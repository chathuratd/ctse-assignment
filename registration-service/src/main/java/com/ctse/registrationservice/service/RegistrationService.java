package com.ctse.registrationservice.service;

import com.ctse.registrationservice.dto.*;
import com.ctse.registrationservice.model.Registration;
import com.ctse.registrationservice.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RegistrationService — Orchestrates the Full Registration Flow
 * ==============================================================
 *
 * This is the most important service in the system from an architecture standpoint.
 * It demonstrates REAL microservice orchestration:
 *
 *   1. VALIDATE USER    → HTTP GET → User Service
 *   2. VALIDATE EVENT   → HTTP GET → Event Service
 *   3. SAVE REGISTRATION → Local DB
 *   4. NOTIFY USER      → HTTP POST → Notification Service
 *
 * INTER-SERVICE COMMUNICATION PATTERN:
 * ──────────────────────────────────────
 * We use RestTemplate (synchronous HTTP client) for simplicity.
 * Each call is sequential — if step 1 fails, steps 2-4 never execute.
 *
 * Service URLs are injected from application.yml:
 *   app.services.user-url=http://user-service:8001
 *
 * In Docker Compose, service names resolve via Docker's internal DNS.
 * "user-service" in the URL resolves to the user-service container's IP.
 *
 * ERROR HANDLING:
 * ───────────────
 * HttpClientErrorException → 4xx from target service (e.g., user not found → 404)
 * RestClientException      → Network error (service down, timeout, etc.)
 * Both are caught and re-thrown with descriptive messages.
 *
 * @Slf4j → Lombok: generates a static Logger field `log`
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final RestTemplate restTemplate;

    // Service URLs injected from application.yml
    @Value("${app.services.user-url}")
    private String userServiceUrl;

    @Value("${app.services.event-url}")
    private String eventServiceUrl;

    @Value("${app.services.notification-url}")
    private String notificationServiceUrl;

    /**
     * Register a user for an event.
     *
     * Full flow:
     *   Step 1 → Validate user exists (via User Service)
     *   Step 2 → Validate event exists (via Event Service)
     *   Step 3 → Check for duplicate registration
     *   Step 4 → Persist registration
     *   Step 5 → Send notification (via Notification Service)
     */
    public RegistrationResponse register(CreateRegistrationRequest request) {

        // ── Step 1: Validate User ──────────────────────────────────────────────
        log.info("Validating user {} via User Service", request.getUserId());
        UserResponse user = validateUser(request.getUserId());

        // ── Step 2: Validate Event ─────────────────────────────────────────────
        log.info("Validating event {} via Event Service", request.getEventId());
        EventResponse event = validateEvent(request.getEventId());

        // ── Step 3: Check for duplicate ────────────────────────────────────────
        boolean alreadyRegistered = registrationRepository.existsByUserIdAndEventIdAndStatus(
                request.getUserId(), request.getEventId(), Registration.RegistrationStatus.CONFIRMED);
        if (alreadyRegistered) {
            throw new IllegalStateException(
                    "User " + user.getName() + " is already registered for " + event.getTitle());
        }

        // ── Step 4: Save Registration ──────────────────────────────────────────
        Registration registration = Registration.builder()
                .userId(request.getUserId())
                .eventId(request.getEventId())
                .status(Registration.RegistrationStatus.CONFIRMED)
                .build();

        Registration saved = registrationRepository.save(registration);
        log.info("Registration {} saved successfully", saved.getId());

        // ── Step 5: Send Notification ──────────────────────────────────────────
        // Notification Service builds the message from the REGISTRATION_CONFIRMED
        // template using userId and eventId to fetch details from User/Event Services.
        sendNotification(request.getUserId(), request.getEventId());

        // ── Return Response ────────────────────────────────────────────────────
        return RegistrationResponse.builder()
                .id(saved.getId())
                .userId(saved.getUserId())
                .eventId(saved.getEventId())
                .status(saved.getStatus().name())
                .createdAt(saved.getCreatedAt())
                .message("Registration successful for " + event.getTitle())
                .build();
    }

    /**
     * Get all registrations for a user.
     * Used by: GET /registrations/user/{userId}
     */
    public List<RegistrationResponse> getUserRegistrations(String userId) {
        return registrationRepository.findByUserId(userId)
                .stream()
                .map(r -> RegistrationResponse.builder()
                        .id(r.getId())
                        .userId(r.getUserId())
                        .eventId(r.getEventId())
                        .status(r.getStatus().name())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();
    }

    /**
     * Cancel a registration by setting status = CANCELLED.
     * Used by: DELETE /registrations/{id}
     *
     * We soft-delete (set status) rather than hard-delete for audit purposes.
     * You can always query cancelled registrations to see historical data.
     */
    public RegistrationResponse cancelRegistration(UUID id) {
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registration not found: " + id));

        if (registration.getStatus() == Registration.RegistrationStatus.CANCELLED) {
            throw new IllegalStateException("Registration is already cancelled");
        }

        registration.setStatus(Registration.RegistrationStatus.CANCELLED);
        Registration updated = registrationRepository.save(registration);

        return RegistrationResponse.builder()
                .id(updated.getId())
                .userId(updated.getUserId())
                .eventId(updated.getEventId())
                .status(updated.getStatus().name())
                .createdAt(updated.getCreatedAt())
                .message("Registration cancelled successfully")
                .build();
    }

    // ── Private helper methods for inter-service calls ─────────────────────────

    /**
     * Calls User Service to validate a user exists.
     *
     * HTTP: GET http://user-service:8001/users/{userId}
     *
     * RestTemplate.getForObject(url, ResponseClass.class):
     *   - Sends GET request
     *   - Parses JSON response into UserResponse object
     *   - Throws HttpClientErrorException on 4xx (e.g., 404 user not found)
     *   - Throws RestClientException on network errors
     */
    private UserResponse validateUser(String userId) {
        String url = userServiceUrl + "/users/" + userId;
        try {
            UserResponse user = restTemplate.getForObject(url, UserResponse.class);
            if (user == null) {
                throw new RuntimeException("User not found: " + userId);
            }
            return user;
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("User not found: " + userId);
        } catch (RestClientException e) {
            log.error("Failed to reach User Service: {}", e.getMessage());
            throw new RuntimeException("User Service is unavailable. Please try again later.");
        }
    }

    /**
     * Calls Event Service to validate an event exists.
     *
     * HTTP: GET http://event-service:8002/events/{eventId}
     */
    private EventResponse validateEvent(String eventId) {
        String url = eventServiceUrl + "/events/" + eventId;
        try {
            EventResponse event = restTemplate.getForObject(url, EventResponse.class);
            if (event == null) {
                throw new RuntimeException("Event not found: " + eventId);
            }
            return event;
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Event not found: " + eventId);
        } catch (RestClientException e) {
            log.error("Failed to reach Event Service: {}", e.getMessage());
            throw new RuntimeException("Event Service is unavailable. Please try again later.");
        }
    }

    /**
     * Calls Notification Service to store a notification.
     *
     * HTTP: POST http://notification-service:8004/notifications
     *       Body: { "type": "REGISTRATION_CONFIRMED", "userId": "...", "eventId": "..." }
     *
     * The Notification Service builds the human-readable message itself by
     * calling User Service (for name) and Event Service (for title/location/date).
     * We only need to supply the IDs and the notification type.
     *
     * Note: Notification failure is non-critical. We log the error but
     * don't fail the entire registration because of it.
     */
    private void sendNotification(String userId, String eventId) {
        String url = notificationServiceUrl + "/notifications";
        try {
            Map<String, String> notificationRequest = Map.of(
                    "type", "REGISTRATION_CONFIRMED",
                    "userId", userId,
                    "eventId", eventId
            );
            restTemplate.postForObject(url, notificationRequest, Object.class);
            log.info("REGISTRATION_CONFIRMED notification sent for user {} on event {}", userId, eventId);
        } catch (RestClientException e) {
            // Non-critical: log warning but don't fail the registration
            log.warn("Failed to send notification (non-critical): {}", e.getMessage());
        }
    }
}
