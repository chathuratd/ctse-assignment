package com.ctse.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserResponse DTO
 * ================
 * What the server returns when sharing user profile information.
 * Used by:
 *   GET /users/me           → current logged-in user's profile
 *   GET /users/{id}         → user by ID (called by Registration Service)
 *
 * Field names match the system API spec:
 *   id   → UUID string (e.g. "550e8400-e29b-41d4-a716-446655440000")
 *   name → user's full name (Registration Service reads this field)
 *
 * Notice: NO password field here. Never return passwords, even hashed ones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String name;       // 'name' matches the inter-service API spec
    private String email;
    private String role;
    private LocalDateTime createdAt;
}
