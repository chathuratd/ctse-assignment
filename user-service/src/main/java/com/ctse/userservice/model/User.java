package com.ctse.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * User Entity
 * ===========
 * This class serves DUAL purposes:
 *
 * 1. JPA ENTITY (@Entity)
 *    - Hibernate will create a "users" table in PostgreSQL from this class.
 *    - Each field annotated with @Column maps to a DB column.
 *    - @Id marks the primary key.
 *
 * 2. SPRING SECURITY UserDetails (implements UserDetails)
 *    - Spring Security calls getUsername(), getPassword(), getAuthorities()
 *      during the login/authentication process.
 *    - This is the BRIDGE between your DB users and Spring Security.
 *
 * Lombok annotations:
 *   @Data       → generates getters, setters, equals, hashCode, toString
 *   @Builder    → enables User.builder().email("...").build() pattern
 *   @NoArgsConstructor & @AllArgsConstructor → required by JPA + Builder
 */
@Entity
@Table(name = "users")          // Table name in PostgreSQL
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    // ── Primary Key ──────────────────────────────────────────────────────────
    // UUID strategy: PostgreSQL generates a universally unique identifier.
    // UUIDs are better than sequential integers for distributed systems:
    //   - No collision risk across services or databases
    //   - Cannot be enumerated (attacker can't guess GET /users/1, /users/2...)
    //   - Standard for microservice architectures
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    // ── User Information ─────────────────────────────────────────────────────
    @Column(nullable = false, length = 100)
    private String fullName;

    // Email is both the unique identifier and the "username" for Spring Security
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // NEVER store plain-text passwords. This column holds a BCrypt hash.
    // BCrypt hash looks like: $2a$10$...  (60 chars)
    @Column(nullable = false)
    private String password;

    // ── Role ─────────────────────────────────────────────────────────────────
    // @Enumerated(STRING) → stores "ROLE_USER" in the DB, not 0 or 1
    // This is safer: if you reorder the enum, the DB data still makes sense
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // ── Audit Fields ─────────────────────────────────────────────────────────
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Automatically set timestamps before INSERT and UPDATE
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── UserDetails contract (required by Spring Security) ───────────────────

    /**
     * getAuthorities() — tells Spring Security what this user is allowed to do.
     * Spring Security uses "GrantedAuthority" objects for role checks.
     * We wrap our Role enum in SimpleGrantedAuthority.
     *
     * This is called internally by Spring Security during request filtering.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    /**
     * getUsername() — Spring Security calls this to get the "username".
     * We use email as the unique identifier (username) for our users.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * The following four methods control account state.
     * For this project we keep all users active (return true).
     * In a production system, you'd add fields like `isEnabled`, `isLocked`
     * to support email verification, account suspension, etc.
     */
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
