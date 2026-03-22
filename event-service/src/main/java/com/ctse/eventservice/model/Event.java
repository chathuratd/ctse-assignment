package com.ctse.eventservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event Entity
 * ============
 * Maps to the "events" table in PostgreSQL.
 * Represents a conference event (AI Workshop, Cybersecurity Seminar, etc.)
 *
 * Fields match the system spec:
 *   id          UUID (auto-generated)
 *   title       Event name
 *   description Brief description
 *   location    Physical location (e.g., "Hall A")
 *   date        When the event takes place (stored as LocalDateTime)
 *   capacity    Maximum number of registrations allowed
 *   createdAt   Auto-set on INSERT
 */
@Entity
@Table(name = "events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 200)
    private String location;

    // Stored as TIMESTAMP in PostgreSQL
    // LocalDateTime = date + time without timezone offset
    @Column(nullable = false)
    private LocalDateTime date;

    // How many users can register for this event
    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
