package com.ctse.eventservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * EventResponse DTO
 * =================
 * Returned by:
 *   POST /events           → event just created
 *   GET /events            → list of all events (each item is EventResponse)
 *   GET /events/{eventId}  → single event (Registration Service calls this to validate)
 *
 * The Registration Service reads:
 *   id    → to store in the registration record
 *   title → to build the notification message: "You registered for AI Workshop"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private UUID id;
    private String title;
    private String description;
    private String location;
    private LocalDateTime date;
    private Integer capacity;
    private LocalDateTime createdAt;
}
