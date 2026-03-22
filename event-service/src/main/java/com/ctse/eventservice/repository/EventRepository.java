package com.ctse.eventservice.repository;

import com.ctse.eventservice.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * EventRepository
 * ===============
 * Spring Data JPA repository for Event entities.
 * Provides built-in: save, findById, findAll, deleteById, existsById, count.
 *
 * The Registration Service calls GET /events/{eventId} which maps to findById(UUID).
 * If not present → 404 is returned, and the registration is rejected.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
}
