package com.ctse.notificationservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * UserMirrorResponse — local copy of User Service's UserResponse.
 *
 * Notification Service calls GET /users/{userId} to fetch name and email
 * so it can personalise notification messages (e.g., "Hi Alice, ...").
 *
 * We define our own copy here — no shared library between services.
 * Only the fields we actually need are included.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMirrorResponse {
    private String id;
    private String name;
    private String email;
}
