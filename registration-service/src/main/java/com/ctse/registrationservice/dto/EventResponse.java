package com.ctse.registrationservice.dto;

import lombok.Data;

/**
 * EventResponse (Mirror DTO)
 * ===========================
 * Used for deserializing the response from Event Service.
 *
 * When Registration Service calls:
 *   GET http://event-service:8002/events/{eventId}
 *
 * Event Service returns:
 *   { "id": "uuid", "title": "AI Workshop", "capacity": 50, ... }
 *
 * We extract the title to build the notification message:
 *   "You have successfully registered for AI Workshop"
 *
 * Same "mirror DTO" reasoning as UserResponse — no cross-service imports.
 */
@Data
public class EventResponse {
    private String id;
    private String title;
    private Integer capacity;
}
