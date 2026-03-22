package com.ctse.eventservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CreateEventRequest DTO
 * ======================
 * Request body for POST /events
 *
 * Example:
 * {
 *   "title": "AI Workshop",
 *   "description": "Introduction to AI and Machine Learning",
 *   "location": "Hall A",
 *   "date": "2026-07-20T10:00:00",
 *   "capacity": 50
 * }
 *
 * @Future validates that the event date is in the future (can't create past events).
 * @Min(1) ensures capacity is at least 1.
 */
@Data
public class CreateEventRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String location;

    @NotNull(message = "Date is required")
    @Future(message = "Event date must be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime date;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;
}
