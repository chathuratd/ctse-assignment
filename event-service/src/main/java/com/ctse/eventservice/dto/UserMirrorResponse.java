package com.ctse.eventservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * UserMirrorResponse — local copy of User Service's UserResponse.
 *
 * Event Service calls GET /users to get all registered users when a new
 * event is created. Each user then receives a NEW_EVENT notification.
 *
 * Only the id field is needed (passed to Notification Service); name is
 * included for logging convenience.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMirrorResponse {
    private String id;
    private String name;
    private String email;
}
