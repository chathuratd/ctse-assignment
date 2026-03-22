package com.ctse.eventservice.service;

import com.ctse.eventservice.dto.CreateEventRequest;
import com.ctse.eventservice.dto.EventResponse;
import com.ctse.eventservice.dto.UserMirrorResponse;
import com.ctse.eventservice.model.Event;
import com.ctse.eventservice.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * EventService — Business Logic for Conference Events
 * =====================================================
 *
 * INTER-SERVICE CALLS (this service is an active caller):
 * ────────────────────────────────────────────────────────
 * When a new event is created:
 *   1. GET  /users                  → User Service (fetch all registered users)
 *   2. POST /notifications (×N)    → Notification Service (one NEW_EVENT per user)
 *
 * The Notification Service uses the eventId and userId to build personalised
 * messages by calling back to User Service and Event Service for details.
 *
 * Failure strategy: notification failures are non-critical and do not roll
 * back event creation. In production this would use a message queue (e.g., SQS).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final RestTemplate restTemplate;

    @Value("${app.services.user-url}")
    private String userServiceUrl;

    @Value("${app.services.notification-url}")
    private String notificationServiceUrl;

    /**
     * Create a new conference event, then notify all registered users.
     *
     * Flow:
     *   1. Save event → get UUID
     *   2. Fetch all users from User Service
     *   3. For each user, POST a NEW_EVENT notification to Notification Service
     */
    public EventResponse createEvent(CreateEventRequest request) {
        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .date(request.getDate())
                .capacity(request.getCapacity())
                .build();

        Event saved = eventRepository.save(event);
        log.info("Event '{}' created with id {}", saved.getTitle(), saved.getId());

        // Notify all users about the new event (non-blocking, best-effort)
        notifyAllUsersAboutNewEvent(saved.getId().toString());

        return toResponse(saved);
    }

    /**
     * List all available conference events.
     */
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get a single event by UUID.
     * Called by Registration Service and Notification Service (inter-service).
     */
    public EventResponse getEventById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        return toResponse(event);
    }

    // ── Inter-service helpers ─────────────────────────────────────────────────

    /**
     * Fetches all users from User Service, then sends a NEW_EVENT notification
     * to Notification Service for each one.
     */
    private void notifyAllUsersAboutNewEvent(String eventId) {
        List<UserMirrorResponse> users = fetchAllUsers();
        if (users.isEmpty()) {
            log.warn("No users found to notify about event {}", eventId);
            return;
        }

        String url = notificationServiceUrl + "/notifications";
        int successCount = 0;
        for (UserMirrorResponse user : users) {
            try {
                Map<String, String> body = Map.of(
                        "type", "NEW_EVENT",
                        "userId", user.getId(),
                        "eventId", eventId
                );
                restTemplate.postForObject(url, body, Object.class);
                successCount++;
            } catch (RestClientException e) {
                log.warn("Could not send NEW_EVENT notification to user {} (non-critical): {}",
                        user.getId(), e.getMessage());
            }
        }
        log.info("Sent NEW_EVENT notifications to {}/{} users for event {}",
                successCount, users.size(), eventId);
    }

    /**
     * Calls User Service: GET /users
     * Returns empty list if the call fails (new event creation is not blocked).
     */
    private List<UserMirrorResponse> fetchAllUsers() {
        String url = userServiceUrl + "/users";
        try {
            ResponseEntity<List<UserMirrorResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            List<UserMirrorResponse> body = response.getBody();
            return body != null ? body : Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("Could not fetch users from User Service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private EventResponse toResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .date(event.getDate())
                .capacity(event.getCapacity())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
