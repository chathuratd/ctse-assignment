package com.ctse.userservice.repository;

import com.ctse.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * UserRepository
 * ==============
 * What is a Repository?
 * ----------------------
 * The Repository is the DATA ACCESS LAYER. It talks directly to the database.
 * You define WHAT queries you need; Spring Data JPA writes the SQL FOR YOU.
 *
 * How does it work?
 * -----------------
 * By extending JpaRepository<User, Long>:
 *   - User  = the entity this repository manages
 *   - Long  = the type of the primary key (@Id field)
 *
 * You instantly get these methods for free (no code needed):
 *   save(user)           → INSERT or UPDATE
 *   findById(id)         → SELECT WHERE id = ?
 *   findAll()            → SELECT * FROM users
 *   delete(user)         → DELETE
 *   existsById(id)       → SELECT EXISTS(...)
 *   count()              → SELECT COUNT(*)
 *
 * Custom queries:
 * ---------------
 * Spring Data JPA parses method names and generates SQL.
 * "findByEmail" → SELECT * FROM users WHERE email = ?
 * "existsByEmail" → SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)
 *
 * We return Optional<User> for findByEmail because the user might not exist.
 * You should ALWAYS check Optional before using the value to avoid NullPointerExceptions.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Used by:
     * - UserDetailsService → to load user from DB during login
     * - AuthService        → to check if email is already registered
     */
    Optional<User> findByEmail(String email);

    /**
     * Used by AuthService during registration to prevent duplicate accounts.
     * More efficient than findByEmail().isPresent() — only runs EXISTS query.
     */
    boolean existsByEmail(String email);
}
