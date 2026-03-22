package com.ctse.registrationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registration Entity
 * ===================
 * Records a user's registration for a conference event.
 *
 * IMPORTANT DESIGN DECISION — No Foreign Keys to Other Services:
 * ──────────────────────────────────────────────────────────────
 * userId and eventId are stored as String (UUID text), NOT as DB foreign keys.
 *
 * Why? Because each microservice owns its own database.
 * The registrations table in THIS service's schema has no FK constraints
 * pointing to the users table (User Service) or events table (Event Service).
 *
 * Instead, the Registration Service validates user+event existence via HTTP
 * calls to the respective services BEFORE saving the registration.
 * This is the microservice pattern: loose coupling via API, not DB constraints.
 *
 * Statuses:
 *   CONFIRMED  → Registration successfully created
 *   CANCELLED  → User cancelled via DELETE /registrations/{id}
 */
@Entity
@Table(name = "registrations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    // UUIDs stored as strings — no cross-service FK constraints
    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 36)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum RegistrationStatus {
        CONFIRMED,
        CANCELLED
    }
}
