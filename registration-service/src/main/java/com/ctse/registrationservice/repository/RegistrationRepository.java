package com.ctse.registrationservice.repository;

import com.ctse.registrationservice.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * RegistrationRepository
 * ======================
 * JPA repository for Registration entities.
 *
 * findByUserId: Finds all registrations for a given user.
 *   Used by GET /registrations/user/{userId}
 *   SQL: SELECT * FROM registrations WHERE user_id = ?
 */
@Repository
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    List<Registration> findByUserId(String userId);

    // Check if a user is already registered for an event (prevent duplicates)
    boolean existsByUserIdAndEventIdAndStatus(
            String userId, String eventId, Registration.RegistrationStatus status);
}
