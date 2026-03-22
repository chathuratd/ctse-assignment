package com.ctse.userservice.service;

import com.ctse.userservice.dto.UserResponse;
import com.ctse.userservice.model.User;
import com.ctse.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * UserService — User Management Business Logic
 * ==============================================
 * Handles operations on existing users (lookup, listing).
 *
 * Inter-service callers:
 *   Registration Service → GET /users/{id}   (validate user exists)
 *   Event Service        → GET /users         (get all users to notify on new event)
 *   Notification Service → GET /users/{id}   (fetch name/email for message template)
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get the current user's profile.
     * The email (username) comes from the JWT extracted by Spring Security.
     */
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return mapToUserResponse(user);
    }

    /**
     * Get a user by their unique ID.
     * Called by Registration Service and Notification Service (inter-service).
     */
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return mapToUserResponse(user);
    }

    /**
     * List all users.
     * Called by Event Service (inter-service) when a new event is created —
     * it needs all user IDs to send a NEW_EVENT notification to each.
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
