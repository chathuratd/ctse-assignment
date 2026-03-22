package com.ctse.notificationservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * EventMirrorResponse — local copy of Event Service's EventResponse.
 *
 * Notification Service calls GET /events/{eventId} to fetch event details
 * so it can include title, location and date in notification messages
 * (e.g., "AI Workshop at Main Hall on 2025-09-01").
 *
 * Only the fields used in notification templates are included.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventMirrorResponse {
    private String id;
    private String title;
    private String location;
    private LocalDateTime date;
}
