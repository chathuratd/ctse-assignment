package com.ctse.userservice.controller;

import com.ctse.userservice.dto.UserResponse;
import com.ctse.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * UserController — REST Endpoints for User Profiles
 * ===================================================
 *
 * Endpoints:
 *   POST /users              → Register a new user (public, handled by AuthController)
 *   GET  /users/me           → Current logged-in user's profile (JWT required)
 *   GET  /users/{id}         → User by UUID (public — called by Registration/Notification Service)
 *   GET  /users              → All users (public — called by Event Service to notify all users)
 *   GET  /admin/users        → All users, admin-only endpoint
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /users/me
     * Returns the profile of the currently authenticated user.
     */
    @GetMapping("/users/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /users/{id}
     *
     * Called by Registration Service and Notification Service (inter-service).
     * Returns user info without the password.
     * Permitted without JWT — secured at the network level in production.
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /users
     *
     * INTER-SERVICE ENDPOINT — called by Event Service when a new event is created.
     * Event Service fetches all users here and then sends each one a NEW_EVENT
     * notification via Notification Service.
     *
     * Permitted without JWT — in production this is restricted at the network level.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * GET /admin/users
     * Lists ALL registered users. Requires ROLE_ADMIN.
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsersAdmin() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}
