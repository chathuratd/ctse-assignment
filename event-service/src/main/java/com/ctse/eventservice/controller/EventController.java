package com.ctse.eventservice.controller;

import com.ctse.eventservice.dto.CreateEventRequest;
import com.ctse.eventservice.dto.EventResponse;
import com.ctse.eventservice.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * EventController — REST Endpoints for Conference Events
 * ========================================================
 *
 * Endpoints (matching the system API spec):
 *
 *   POST /events             Create a new event
 *   GET  /events             List all events
 *   GET  /events/{eventId}   Get event by UUID
 *
 * No authentication required — events are publicly readable.
 * In a production system, POST /events would require an admin JWT.
 *
 * Inter-service usage:
 *   Registration Service calls GET /events/{eventId} to validate an event
 *   exists before creating a registration record.
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * POST /events
     *
     * Creates a new conference event.
     * Request: { "title": "AI Workshop", "description": "...", "location": "Hall A",
     *            "date": "2026-07-20T10:00:00", "capacity": 50 }
     * Response (201 Created): EventResponse with generated UUID
     */
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEvent(request));
    }

    /**
     * GET /events
     *
     * Returns all events. Used by the frontend events browse page.
     * Response: [ { "id": "...", "title": "AI Workshop", "date": "..." }, ... ]
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    /**
     * GET /events/{eventId}
     *
     * Returns a single event by UUID.
     * Called by Registration Service during the register-for-event flow.
     *
     * If event not found → 404 (handled by GlobalExceptionHandler)
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }
}
