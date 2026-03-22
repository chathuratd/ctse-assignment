package com.ctse.userservice.model;

/**
 * What is a "Role"?
 * -----------------
 * Roles are used for Authorization — deciding WHAT an authenticated user is allowed to do.
 *
 * Authentication = "Who are you?"   → login with email/password
 * Authorization  = "What can you do?" → ROLE_ADMIN can delete users, ROLE_USER cannot
 *
 * Spring Security reads these roles and uses them in your SecurityConfig to restrict
 * endpoints. Example: .requestMatchers("/admin/**").hasRole("ADMIN")
 */
public enum Role {
    ROLE_USER,   // Default role for all registered users
    ROLE_ADMIN   // Elevated role for conference administrators
}
